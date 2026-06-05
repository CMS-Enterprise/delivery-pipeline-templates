def call(Map config = [:]) {
    def url = config.url ?: config.health_endpoint ?: "${env.DEPLOY_URL}/health"
    def retries = config.retries ?: 5
    def delay = config.retry_delay_seconds ?: 10
    def expected_status = config.expected_status ?: 200

    stage("Smoke Test") {
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
