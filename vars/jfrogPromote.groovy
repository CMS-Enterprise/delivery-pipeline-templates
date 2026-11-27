def call(Map config = [:], String target = null) {
    def target_repo = target ?: config.prod_repo ?: 'docker-prod-local'
    def server_id = config.server_id ?: 'jfrog-artifactory'
    def source_repo = config.staging_repo ?: 'docker-staging-local'
    def jfrog_url = config.url ?: 'https://artifactory.cloud.cms.gov/artifactory'
    def copy = config.copy ?: false
    def build_name = config.build_name ?: env.JFROG_BUILD_NAME ?: env.JOB_NAME
    def build_number = config.build_number ?: env.JFROG_BUILD_NUMBER ?: env.BUILD_NUMBER
    def stagename = config.stage ?: "Promote to ${target_repo}"
    def project_flag = config.project ? "--project=${config.project}" : ''
    def promoted = null

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/jfrog-cli.yaml')) {
            node(POD_LABEL) {
                container('jfrog-cli') {
                    withCredentials([usernamePassword(credentialsId: config.credential ?: 'jfrog-credentials', usernameVariable: 'JFROG_USER', passwordVariable: 'JFROG_ACCESS_TOKEN')]) {
                        sh """
                            jf config add ${server_id} \
                                --url=${jfrog_url} \
                                --user=\$JFROG_USER \
                                --access-token=\$JFROG_ACCESS_TOKEN \
                                --interactive=false \
                                --overwrite=true

                            jf rt build-promote '${build_name}' ${build_number} ${target_repo} \
                                --server-id=${server_id} \
                                --source-repo=${source_repo} \
                                --status=Released \
                                --comment="Promoted after passing Xray scan" \
                                ${copy ? '--copy=true' : ''} ${project_flag}
                        """
                        // build-promote reports success even when the build info lists
                        // no artifacts, so confirm the image actually reached the target.
                        if (config.image) {
                            def ref = config.image.replace(source_repo, target_repo)
                            def m = (ref =~ /^[^\/]+\/([^\/]+)\/(.+):([^:@\/]+)$/)
                            if (!m.matches()) {
                                error("jfrogPromote: cannot parse image reference '${ref}'")
                            }
                            def manifest = "${m.group(1)}/${m.group(2)}/${m.group(3)}/*manifest.json"
                            m = null
                            def found = sh(
                                script: "jf rt search '${manifest}' --server-id=${server_id} --count",
                                returnStdout: true
                            ).trim()
                            if (!(found ==~ /[1-9][0-9]*/)) {
                                error("jfrogPromote: ${manifest} not found after promotion (count: ${found}); " +
                                    "the build info for '${build_name}' #${build_number} likely records no image")
                            }
                        }
                    }
                    // Callers that fan out pass an explicit image and use the
                    // return value, since a shared env var would race.
                    if (config.image) {
                        promoted = config.image.replace(source_repo, target_repo)
                    } else {
                        env.IMAGE_TAG = env.IMAGE_TAG.replace(source_repo, target_repo)
                        promoted = env.IMAGE_TAG
                    }
                    echo "Image promoted to ${target_repo}: ${promoted}"
                }
            }
        }
    }
    promoted
}
