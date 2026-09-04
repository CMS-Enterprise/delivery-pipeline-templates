def call(Map config = [:]) {
    def server_id = config.server_id ?: 'jfrog-artifactory'
    def jfrog_url = config.url ?: 'https://artifactory.cloud.cms.gov/artifactory'
    def output_name = config.output_name ?: 'jfrog-sbom'
    def build_name = config.build_name ?: env.JFROG_BUILD_NAME ?: env.JOB_NAME
    def build_number = config.build_number ?: env.JFROG_BUILD_NUMBER ?: env.BUILD_NUMBER
    def stagename = config.stage ?: 'JFrog SBOM Export'

    stage("${stagename}") {
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

                            jf rt build-collect-env ${build_name} ${build_number}

                            jf sbom-export \
                                --server-id=${server_id} \
                                --build-name=${build_name} \
                                --build-number=${build_number} \
                                --format=cyclonedx \
                                > ${output_name}.cyclonedx.json
                        """
                    }
                    archiveArtifacts artifacts: "${output_name}.cyclonedx.json"
                }
            }
        }
    }
}
