def call(Map config = [:]) {
    def conftest_image = config.conftest_image ?: 'artifactory.cloud.cms.gov/docker/openpolicyagent/conftest@sha256:a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3'
    def policy_path = config.policy_path ?: 'policy/'
    def input_paths = config.input_paths ?: '.'
    def namespace = config.namespace ?: 'main'
    def output_format = config.output_format ?: 'json'

    stage("OPA Policy Check") {
        podTemplate(containers: [
            containerTemplate(name: 'conftest', image: conftest_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                unstash "workspace"
                container('conftest') {
                    sh """
                        conftest test ${input_paths} \
                            --policy ${policy_path} \
                            --namespace ${namespace} \
                            --output ${output_format} \
                            | tee conftest-results.json
                    """
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "conftest-results.json"
            }
        }
    }
}
