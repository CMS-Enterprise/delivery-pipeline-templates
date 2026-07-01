def call(Map config = [:]) {
    def policy_path = config.policy_path ?: 'policy/'
    def input_paths = config.input_paths ?: '.'
    def namespace = config.namespace ?: 'main'
    def output_format = config.output_format ?: 'json'

    stage("OPA Policy Check") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/conftest.yaml')) {
            node(POD_LABEL) {
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
