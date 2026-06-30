def call(Map config = [:]) {
    def config_file = config.config_file ?: '.golangci.yml'
    def timeout = config.timeout ?: '5m'
    def fail_on_error = config.fail_on_error != false

    stage("golangci-lint") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/golangci-lint.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('golangci-lint') {
                    def exit_code = sh(
                        script: """
                            golangci-lint run \
                                --config ${config_file} \
                                --timeout ${timeout} \
                                --out.json.path golangci-lint-results.json
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
