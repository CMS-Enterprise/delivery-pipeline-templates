def call(Map config = [:], String target = null) {
    def target_repo = target ?: config.prod_repo ?: 'docker-prod-local'
    def jfrog_cli_image = config.jfrog_cli_image ?: 'releases-docker.jfrog.io/jfrog/jfrog-cli-v2@sha256:4a7d5c8e9f2b1d6a3c8e5f7b0d2a4e6c9f1b3d5a7c9e2f4b6d8a0c3e5f7a9b1d'
    def server_id = config.server_id ?: 'jfrog-artifactory'
    def source_repo = config.staging_repo ?: 'docker-staging-local'
    def jfrog_url = config.url ?: 'https://artifactory.cloud.cms.gov/artifactory'
    def copy = config.copy ?: false

    stage("Promote to ${target_repo}") {
        podTemplate(containers: [
            containerTemplate(name: 'jfrog-cli', image: jfrog_cli_image, command: 'cat', ttyEnabled: true)
        ]) {
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
