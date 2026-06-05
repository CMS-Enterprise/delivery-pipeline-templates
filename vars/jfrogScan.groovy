def call(Map config = [:]) {
    def server_id = config.server_id ?: 'jfrog-artifactory'
    def jfrog_url = config.url ?: 'https://artifactory.cloud.cms.gov/artifactory'
    def fail_on_violation = config.fail_on_violation != false

    stage("JFrog Xray Scan") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/jfrog-cli.yaml')) {
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
