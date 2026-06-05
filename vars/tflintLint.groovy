def call(Map config = [:]) {
    def tflint_image = config.tflint_image ?: 'artifactory.cloud.cms.gov/docker/ghcr/terraform-linters/tflint@sha256:a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2'
    def terraform_dir = config.terraform_dir ?: 'terraform/'
    def fail_on_error = config.fail_on_error != false
    def minimum_severity = config.minimum_severity ?: 'warning'

    stage("TFLint") {
        podTemplate(containers: [
            containerTemplate(name: 'tflint', image: tflint_image, command: 'cat', ttyEnabled: true)
        ]) {
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
