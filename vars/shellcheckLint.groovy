def call(Map config = [:]) {
    def shellcheck_image = config.shellcheck_image ?: 'artifactory.cloud.cms.gov/docker/koalaman/shellcheck-alpine@sha256:b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8'
    def paths = config.paths ?: 'scripts/'
    def severity = config.severity ?: 'warning'
    def shell_dialect = config.shell ?: 'bash'
    def fail_on_error = config.fail_on_error != false

    stage("ShellCheck Lint") {
        podTemplate(containers: [
            containerTemplate(name: 'shellcheck', image: shellcheck_image, command: 'cat', ttyEnabled: true)
        ]) {
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
