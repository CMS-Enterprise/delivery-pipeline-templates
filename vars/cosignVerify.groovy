def call(Map config = [:]) {
    def kms_key_arn = config.kms_key_arn
    if (!kms_key_arn) {
        echo "No KMS key ARN provided — skipping cosign verify"
        return
    }

    def image = env.IMAGE_TAG ?: error("IMAGE_TAG not set")

    stage("Cosign Verify") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/cosign.yaml')) {
            node(POD_LABEL) {
                container('cosign') {
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
