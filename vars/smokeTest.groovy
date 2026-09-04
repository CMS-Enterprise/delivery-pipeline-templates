def call(Map config = [:]) {
    def url = config.url ?: config.health_endpoint ?: "${env.DEPLOY_URL}/health"
    // Template parameters arrive as strings; retry()/sleep() need numbers.
    def retries = (config.retries ?: 5) as Integer
    def delay = (config.retry_delay_seconds ?: 10) as Integer
    def expected_status = config.expected_status ?: 200
    def stagename = config.stage ?: 'Smoke Test'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/curl.yaml')) {
            node(POD_LABEL) {
                container('curl') {
                    retry(retries) {
                        sleep delay
                        sh "curl -sf -o /dev/null -w '%{http_code}' ${url} | grep -q '${expected_status}'"
                    }
                    echo "Smoke test passed: ${url} returned ${expected_status}"
                }
            }
        }
    }
}
