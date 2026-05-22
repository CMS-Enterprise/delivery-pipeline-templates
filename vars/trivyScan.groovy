def call(Map config = [:]) {
    def trivy_image = config.trivy_image ?: 'artifactory.cloud.cms.gov/docker/aquasec/trivy@sha256:b9e3d0c3e0c3f9d6a8e5a7c2f1b0a4d8e6c9f2a5b8d1e4f7a0c3b6d9e2f5a8b1'
    def severity = config.severity ?: 'CRITICAL,HIGH'
    def exit_code = config.fail_on_vulnerability ? '1' : '0'
    def ignore_unfixed = config.ignore_unfixed != null ? config.ignore_unfixed : true

    stage("Trivy Container Scan") {
        podTemplate(containers: [
            containerTemplate(name: 'trivy', image: trivy_image, command: 'cat', ttyEnabled: true)
        ]) {
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
