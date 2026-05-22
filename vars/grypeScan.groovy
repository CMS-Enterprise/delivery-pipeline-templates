def call(Map config = [:]) {
    def grype_image = config.grype_image ?: 'artifactory.cloud.cms.gov/docker/anchore/grype@sha256:b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3'
    def fail_on_severity = config.fail_on_severity ?: 'high'
    def only_fixed = config.only_fixed ?: false
    def output_format = config.output_format ?: 'table'

    stage("Grype Container Scan") {
        podTemplate(containers: [
            containerTemplate(name: 'grype', image: grype_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                container('grype') {
                    def image = env.IMAGE_TAG ?: error("IMAGE_TAG not set — run build step first")
                    sh """
                        grype ${image} \
                            --fail-on ${fail_on_severity} \
                            ${only_fixed ? '--only-fixed' : ''} \
                            -o ${output_format} \
                            -o json=grype-results.json
                    """
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "grype-results.json"
            }
        }
    }
}
