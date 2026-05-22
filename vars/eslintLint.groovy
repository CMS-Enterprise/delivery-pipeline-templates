def call(Map config = [:]) {
    def node_image = config.node_image ?: "artifactory.cloud.cms.gov/docker/node:${config.node_version ?: '20'}"
    def paths = config.paths ?: 'src/'
    def fail_on_error = config.fail_on_error != false

    stage("ESLint") {
        podTemplate(containers: [
            containerTemplate(name: 'node', image: node_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                unstash "workspace"
                container('node') {
                    def exit_code = sh(
                        script: "npx eslint ${paths} --format json --output-file eslint-results.json",
                        returnStatus: true
                    )
                    if (exit_code != 0 && fail_on_error) {
                        error "ESLint found violations"
                    }
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "eslint-results.json"
            }
        }
    }
}
