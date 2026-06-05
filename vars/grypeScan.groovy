def call(Map config = [:]) {
    def fail_on_severity = config.fail_on_severity ?: 'high'
    def only_fixed = config.only_fixed ?: false
    def output_format = config.output_format ?: 'table'

    stage("Grype Container Scan") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/grype.yaml')) {
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
