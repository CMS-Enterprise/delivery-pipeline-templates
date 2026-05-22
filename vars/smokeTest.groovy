def call(Map config = [:]) {
    def curl_image = config.curl_image ?: 'artifactory.cloud.cms.gov/docker/curlimages/curl@sha256:eb411f0a02b75f2c2342dbc2f6579905979dd65f61f1b3047067829bb553d149'
    def url = config.url ?: config.health_endpoint ?: "${env.DEPLOY_URL}/health"
    def retries = config.retries ?: 5
    def delay = config.retry_delay_seconds ?: 10
    def expected_status = config.expected_status ?: 200

    stage("Smoke Test") {
        podTemplate(containers: [
            containerTemplate(name: 'curl', image: curl_image, command: 'cat', ttyEnabled: true)
        ]) {
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
