def call(Map config = [:]) {
    def terraform_dir = config.terraform_dir ?: 'terraform/'
    def fail_on_error = config.fail_on_error != false
    def minimum_severity = config.minimum_severity ?: 'warning'

    stage("TFLint") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/tflint.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('tflint') {
                    dir(terraform_dir) {
                        def exit_code = sh(
                            script: """
                                tflint --init
                                tflint \
                                    --minimum-failure-severity=${minimum_severity} \
                                    --format=json > tflint-results.json
                            """,
                            returnStatus: true
                        )
                        if (exit_code != 0 && fail_on_error) {
                            error "TFLint found issues"
                        }
                    }
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "**/tflint-results.json"
            }
        }
    }
}
