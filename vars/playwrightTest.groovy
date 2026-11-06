def call(Map config = [:]) {
    def base_url = config.base_url ?: env.DEPLOY_URL
    // An empty selenium_url runs the browsers bundled in the playwright image,
    // which is required when the target is only reachable from inside the pod.
    def selenium_url = config.containsKey('selenium_url') ? config.selenium_url : 'https://selenium.cloud.cms.gov/'
    def project = config.project ?: ''
    def workers = config.workers ?: 4
    def working_dir = config.working_dir ?: '.'
    def myunstash = config.unstash ?: 'workspace'
    def output_name = config.output_name ?: 'playwright'
    def stagename = config.stage ?: 'Playwright Integration Test'
    def registry = config.npm_registry ?: 'https://artifactory.cloud.cms.gov/artifactory/api/npm/npm/'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/playwright.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                def status = 0
                container('playwright') {
                    status = sh(returnStatus: true, script: """
                        cd ${working_dir}
                        export CI=1
                        export npm_config_cache="\$PWD/.npm"
                        npm ci --registry=${registry}
                        ${selenium_url ? "export SELENIUM_REMOTE_URL='${selenium_url}'" : ''}
                        ${base_url ? "export BASE_URL='${base_url}'" : ''}
                        export PLAYWRIGHT_JUNIT_OUTPUT_NAME='${output_name}-junit.xml'
                        export PLAYWRIGHT_HTML_OUTPUT_DIR='${output_name}-report'
                        export PLAYWRIGHT_HTML_OPEN=never
                        npx playwright test \
                            --reporter=junit,html \
                            --workers=${workers} \
                            ${project ? "--project=${project}" : ''}
                    """)
                }
                junit allowEmptyResults: true, testResults: "${working_dir}/${output_name}-junit.xml"
                // archiveArtifacts, not publishHTML: the HTML Publisher plugin is
                // not installed on the controller, so publishHTML throws
                // NoSuchMethodError and fails the stage after the tests pass.
                archiveArtifacts allowEmptyArchive: true, artifacts: "${working_dir}/${output_name}-report/**"
                if (status != 0) {
                    error "Playwright tests failed (exit ${status})"
                }
            }
        }
    }
}
