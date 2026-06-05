def call(Map config = [:], String target = null) {
    def target_repo = target ?: config.prod_repo ?: 'docker-prod-local'
    def server_id = config.server_id ?: 'jfrog-artifactory'
    def source_repo = config.staging_repo ?: 'docker-staging-local'
    def jfrog_url = config.url ?: 'https://artifactory.cloud.cms.gov/artifactory'
    def copy = config.copy ?: false

    stage("Promote to ${target_repo}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/jfrog-cli.yaml')) {
            node(POD_LABEL) {
                container('jfrog-cli') {
                    withCredentials([usernamePassword(credentialsId: config.credential ?: 'jfrog-credentials', usernameVariable: 'JFROG_USER', passwordVariable: 'JFROG_PASS')]) {
                        sh """
                            jf config add ${server_id} \
                                --url=${jfrog_url} \
                                --user=\$JFROG_USER \
                                --password=\$JFROG_PASS \
                                --interactive=false \
                                --overwrite=true

                            jf rt build-promote ${env.JFROG_BUILD_NAME ?: env.JOB_NAME} ${env.JFROG_BUILD_NUMBER ?: env.BUILD_NUMBER} ${target_repo} \
                                --server-id=${server_id} \
                                --source-repo=${source_repo} \
                                --status=Released \
                                --comment="Promoted after passing Xray scan" \
                                ${copy ? '--copy=true' : ''}
                        """
                    }
                    env.IMAGE_TAG = env.IMAGE_TAG.replace(source_repo, target_repo)
                    echo "Image promoted to ${target_repo}: ${env.IMAGE_TAG}"
                }
            }
        }
    }
}
