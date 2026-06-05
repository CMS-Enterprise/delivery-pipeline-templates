def call(Map config = [:]) {
    def snyk_image = config.snyk_image ?: 'artifactory.cloud.cms.gov/docker/snyk/snyk@sha256:9d890769442afa9bd185403e7c199a60ea70b6784865e9ed0df2380f45e1c028'
    def severity_threshold = config.severity_threshold ?: 'high'
    def org = config.org ?: env.SNYK_ORG
    def image = config.image ?: env.IMAGE_TAG ?: error("image is required")
    def output_name = config.output_name ?: 'snyk-container'
    def generate_sbom = config.generate_sbom != false

    stage("Snyk Container Scan") {
        podTemplate(containers: [
            containerTemplate(name: 'snyk', image: snyk_image, command: 'cat', ttyEnabled: true)
        ]) {
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
