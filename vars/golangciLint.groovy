def call(Map config = [:]) {
    def golang_image = config.golang_image ?: 'artifactory.cloud.cms.gov/docker/golangci/golangci-lint@sha256:a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3'
    def config_file = config.config_file ?: '.golangci.yml'
    def timeout = config.timeout ?: '5m'
    def fail_on_error = config.fail_on_error != false

    stage("golangci-lint") {
        podTemplate(containers: [
            containerTemplate(name: 'golangci-lint', image: golang_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                checkout scm
                container('golangci-lint') {
                    def exit_code = sh(
                        script: """
                            golangci-lint run \
                                --config ${config_file} \
                                --timeout ${timeout} \
                                --out-format json > golangci-lint-results.json
                        """,
                        returnStatus: true
                    )
                    if (exit_code != 0 && fail_on_error) {
                        error "golangci-lint found issues"
                    }
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "golangci-lint-results.json"
            }
        }
    }
}
