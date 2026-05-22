def call(Map config = [:]) {
    def kms_key_arn = config.kms_key_arn
    if (!kms_key_arn) {
        echo "No KMS key ARN provided — skipping cosign verify"
        return
    }

    def cosign_image = config.cosign_image ?: 'artifactory.cloud.cms.gov/docker/sigstore/cosign@sha256:c1b2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2'
    def image = env.IMAGE_TAG ?: error("IMAGE_TAG not set")

    stage("Cosign Verify") {
        podTemplate(containers: [
            containerTemplate(name: 'cosign', image: cosign_image, command: 'cat', ttyEnabled: true)
        ]) {
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
