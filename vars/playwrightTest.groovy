def call(Map config = [:]) {
    def playwright_image = config.playwright_image ?: 'artifactory.cloud.cms.gov/docker/playwright@sha256:f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6'
    def base_url = config.base_url ?: env.DEPLOY_URL
    def 
    def project = config.project ?: ''
    def workers = config.workers ?: 4

    stage("Playwright Integration Test") {
        podTemplate(containers: [
            containerTemplate(name: 'playwright', image: playwright_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                unstash "workspace"
                container('playwright') {
                    sh """
                        export SELENIUM_REMOTE_URL="https://seleniumbox.cloud.cms.gov/"
                        npx playwright test \
                            --reporter=junit,html \
                            --workers=${workers} \
                            ${project ? "--project=${project}" : ''} \
                            ${base_url ? "BASE_URL=${base_url}" : ''}
                    """
                }
                junit allowEmptyResults: true, testResults: 'test-results/junit.xml'
                publishHTML(target: [
                    reportDir: "playwright-report",
                    reportFiles: "index.html",
                    reportName: "Playwright Report"
                ])
            }
        }
    }
}
