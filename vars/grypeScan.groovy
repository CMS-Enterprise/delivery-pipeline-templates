def call(Map config = [:]) {
    def image = config.image ?: env.IMAGE_TAG ?: error("image is required (env.IMAGE_TAG is not set)")
    def fail_on_severity = config.fail_on_severity ?: 'high'
    def only_fixed = config.only_fixed ?: false
    def output_name = config.output_name ?: 'grype'
    def stagename = config.stage ?: 'Grype Container Scan'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/grype.yaml')) {
            node(POD_LABEL) {
                container('grype') {
                    sh """
                        grype ${image} \
                            --fail-on ${fail_on_severity} \
                            ${only_fixed ? '--only-fixed' : ''} \
                            -o json=${output_name}-results.json
                    """
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "${output_name}-results.json"
            }
        }
    }
}
