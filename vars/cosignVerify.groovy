def call(Map config = [:]) {
    def kms_key_arn = config.kms_key_arn
    if (!kms_key_arn) {
        echo "No KMS key ARN provided — skipping cosign verify"
        return
    }
    def image = config.image ?: env.IMAGE_TAG ?: error("image is required (env.IMAGE_TAG is not set)")
    def stagename = config.stage ?: "Cosign Verify"

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/cosign.yaml')) {
            node(POD_LABEL) {
                container('cosign') {
                    // TODO: switch into AWS account
                    sh """
                        cosign verify --key awskms:///${kms_key_arn} \
                            --insecure-ignore-tlog=true \
                            ${image}
                    """
                }
            }
        }
    }
}
