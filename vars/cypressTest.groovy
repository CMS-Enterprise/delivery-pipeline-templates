def call(Map config = [:]) {
    def base_url = config.base_url ?: env.DEPLOY_URL
    def browser = config.browser ?: 'chrome'
    def spec_pattern = config.spec_pattern ?: 'cypress/e2e/**/*.cy.ts'
    def parallel = config.parallel ?: false

    stage("Cypress Integration Test") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/cypress.yaml')) {
            node(POD_LABEL) {
                container('cypress') {
                    sh """
                        npx cypress run \
                            --browser ${browser} \
                            --config baseUrl=${base_url} \
                            --spec '${spec_pattern}' \
                            ${parallel ? '--parallel' : ''}
                    """
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "cypress/videos/**,cypress/screenshots/**"
                junit allowEmptyResults: true, testResults: 'cypress/results/*.xml'
            }
        }
    }
}
