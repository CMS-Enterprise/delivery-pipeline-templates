def call(Map config = [:]) {
    def jfrog_cli_image = config.jfrog_cli_image ?: 'releases-docker.jfrog.io/jfrog/jfrog-cli-v2@sha256:4a7d5c8e9f2b1d6a3c8e5f7b0d2a4e6c9f1b3d5a7c9e2f4b6d8a0c3e5f7a9b1d'
    def server_id = config.server_id ?: 'jfrog-artifactory'
    def jfrog_url = config.url ?: 'https://artifactory.cloud.cms.gov/artifactory'
    def fail_on_violation = config.fail_on_violation != false

    stage("JFrog Xray Scan") {
        podTemplate(containers: [
            containerTemplate(name: 'jfrog-cli', image: jfrog_cli_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                cosignVerify(config)
                container('jfrog-cli') {
                    withCredentials([usernamePassword(credentialsId: config.credential ?: 'jfrog-credentials', usernameVariable: 'JFROG_USER', passwordVariable: 'JFROG_PASS')]) {
                        sh """
                            jf config add ${server_id} \
                                --url=${jfrog_url} \
                                --user=\$JFROG_USER \
                                --password=\$JFROG_PASS \
                                --interactive=false \
                                --overwrite=true

                            jf build-scan ${env.JFROG_BUILD_NAME ?: env.JOB_NAME} ${env.JFROG_BUILD_NUMBER ?: env.BUILD_NUMBER} \
                                --server-id=${server_id} \
                                ${fail_on_violation ? '--fail=true' : '--fail=false'}
                        """
                    }
                }
            }
        }
    }
}
