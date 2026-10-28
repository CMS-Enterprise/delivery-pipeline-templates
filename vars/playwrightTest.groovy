def call(Map config = [:]) {
    def base_url = config.base_url ?: env.DEPLOY_URL
    def selenium_url = config.selenium_url ?: 'https://selenium.cloud.cms.gov/'
    def project = config.project ?: ''
    def workers = config.workers ?: 4

    stage("Playwright Integration Test") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/playwright.yaml')) {
            node(POD_LABEL) {
                unstash "workspace"
                container('playwright') {
                    sh """
                        export SELENIUM_REMOTE_URL="${selenium_url}"
                        npx playwright test \
                            --reporter=junit,html \
                            --workers=${workers} \
                            ${project ? "--project=${project}" : ''} \
                            ${base_url ? "BASE_URL=${base_url}" : ''}
                    """
                }
                junit allowEmptyResults: true, testResults: 'test-results/junit.xml'
                // archiveArtifacts, not publishHTML: the HTML Publisher plugin is
                // not installed on the controller, so publishHTML throws
                // NoSuchMethodError and fails the stage after the tests pass.
                archiveArtifacts allowEmptyArchive: true, artifacts: 'playwright-report/**'
            }
        }
    }
}
