def call(Map config = [:]) {
    def severity = config.severity ?: 'CRITICAL,HIGH'
    def exit_code = config.fail_on_vulnerability ? '1' : '0'
    def ignore_unfixed = config.ignore_unfixed != null ? config.ignore_unfixed : true

    stage("Trivy Container Scan") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/trivy.yaml')) {
            node(POD_LABEL) {
                cosignVerify(config)
                container('trivy') {
                    def image = env.IMAGE_TAG ?: error("IMAGE_TAG not set — run build step first")
                    sh """
                        trivy image ${image} \
                            --severity ${severity} \
                            --exit-code ${exit_code} \
                            ${ignore_unfixed ? '--ignore-unfixed' : ''} \
                            --format json \
                            --output trivy-results.json
                    """
                    archiveArtifacts allowEmptyArchive: true, artifacts: "trivy-results.json"
                }
            }
        }
    }
}
