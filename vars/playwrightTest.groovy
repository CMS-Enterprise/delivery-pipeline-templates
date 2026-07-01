def call(Map config = [:]) {
    def base_url = config.base_url ?: env.DEPLOY_URL
    def project = config.project ?: ''
    def workers = config.workers ?: 4

    stage("Playwright Integration Test") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/playwright.yaml')) {
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
