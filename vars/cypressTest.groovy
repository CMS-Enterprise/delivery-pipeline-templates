def call(Map config = [:]) {
    def cypress_image = config.cypress_image ?: 'artifactory.cloud.cms.gov/docker/cypress/included@sha256:d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2'
    def base_url = config.base_url ?: env.DEPLOY_URL
    def browser = config.browser ?: 'chrome'
    def spec_pattern = config.spec_pattern ?: 'cypress/e2e/**/*.cy.ts'
    def parallel = config.parallel ?: false

    stage("Cypress Integration Test") {
        podTemplate(containers: [
            containerTemplate(name: 'cypress', image: cypress_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                unstash "workspace"
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
