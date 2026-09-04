def call(Map config = [:]) {
    def severity = config.severity ?: 'CRITICAL,HIGH'
    def exit_code = config.fail_on_vulnerability ? '1' : '0'
    def ignore_unfixed = config.ignore_unfixed != null ? config.ignore_unfixed : true
    def image = config.image ?: env.IMAGE_TAG ?: error("image is required (env.IMAGE_TAG is not set)")
    def output_name = config.output_name ?: 'trivy'
    def stagename = config.stage ?: 'Trivy Container Scan'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/trivy.yaml')) {
            node(POD_LABEL) {
                // Callers that already verified the signature pass skip_verify to
                // avoid paying for a second cosign pod per scanner.
                if (!config.skip_verify) {
                    cosignVerify(config + [image: image])
                }
                container('trivy') {
                    sh """
                        trivy image ${image} \
                            --severity ${severity} \
                            --exit-code ${exit_code} \
                            ${ignore_unfixed ? '--ignore-unfixed' : ''} \
                            --format json \
                            --output ${output_name}-results.json
                    """
                    archiveArtifacts allowEmptyArchive: true, artifacts: "${output_name}-results.json"
                }
            }
        }
    }
}
