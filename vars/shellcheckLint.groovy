def call(Map config = [:]) {
    def paths = config.paths ?: 'scripts/'
    def severity = config.severity ?: 'warning'
    def shell_dialect = config.shell ?: 'bash'
    def fail_on_error = config.fail_on_error != false

    stage("ShellCheck Lint") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/shellcheck.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('shellcheck') {
                    def exit_code = sh(
                        script: """
                            find ${paths} -name '*.sh' -print0 | \
                                xargs -0 shellcheck \
                                    --severity=${severity} \
                                    --shell=${shell_dialect} \
                                    --format=json > shellcheck-results.json
                        """,
                        returnStatus: true
                    )
                    if (exit_code != 0 && fail_on_error) {
                        error "ShellCheck found issues"
                    }
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "shellcheck-results.json"
            }
        }
    }
}
