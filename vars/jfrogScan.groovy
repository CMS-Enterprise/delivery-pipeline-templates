def call(Map config = [:]) {
    def server_id = config.server_id ?: 'jfrog-artifactory'
    def jfrog_url = config.url ?: 'https://artifactory.cloud.cms.gov/artifactory'
    def fail_on_violation = config.fail_on_violation != false
    def stagename = config.stage ?: 'JFrog Xray Scan'
    def build_name = config.build_name ?: env.JFROG_BUILD_NAME ?: env.JOB_NAME
    def build_number = config.build_number ?: env.JFROG_BUILD_NUMBER ?: env.BUILD_NUMBER

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/jfrog-cli.yaml')) {
            node(POD_LABEL) {
                if (!config.skip_verify) {
                    cosignVerify(config)
                }
                container('jfrog-cli') {
                    withCredentials([usernamePassword(credentialsId: config.credential ?: 'jfrog-credentials', usernameVariable: 'JFROG_USER', passwordVariable: 'JFROG_PASS')]) {
                        sh """
                            jf config add ${server_id} \
                                --url=${jfrog_url} \
                                --user=\$JFROG_USER \
                                --password=\$JFROG_PASS \
                                --interactive=false \
                                --overwrite=true

                            jf build-scan ${build_name} ${build_number} \
                                --server-id=${server_id} \
                                ${fail_on_violation ? '--fail=true' : '--fail=false'}
                        """
                    }
                }
            }
        }
    }
}
