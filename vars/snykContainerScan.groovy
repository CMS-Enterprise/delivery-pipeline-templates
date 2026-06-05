def call(Map config = [:]) {
    def severity_threshold = config.severity_threshold ?: 'high'
    def org = config.org ?: env.SNYK_ORG
    def image = config.image ?: env.IMAGE_TAG ?: error("image is required")
    def output_name = config.output_name ?: 'snyk-container'
    def generate_sbom = config.generate_sbom != false

    stage("Snyk Container Scan") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/snyk.yaml')) {
            node(POD_LABEL) {
                container('snyk') {
                    withCredentials([string(credentialsId: config.token_credential ?: 'snyk-api-token', variable: 'SNYK_TOKEN')]) {
                        sh """
                            snyk auth \$SNYK_TOKEN
                            snyk container test ${image} \
                                --severity-threshold=${severity_threshold} \
                                --org=${org} \
                                --json-file-output=${output_name}-results.json
                        """
                        if (generate_sbom) {
                            sh """
                                snyk container sbom ${image} \
                                    --org=${org} \
                                    --format=cyclonedx1.4+json > ${output_name}-sbom.cyclonedx.json
                            """
                        }
                    }
                    archiveArtifacts artifacts: "${output_name}-*.json"
                }
            }
        }
    }
}
