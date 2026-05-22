def call(Map config = [:]) {
    def kms_key_arn = config.kms_key_arn
    if (!kms_key_arn) {
        echo "No KMS key ARN provided — skipping cosign sign"
        return
    }

    def cosign_image = config.cosign_image ?: 'artifactory.cloud.cms.gov/docker/sigstore/cosign@sha256:c1b2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2'
    def aws_image = config.aws_image ?: 'artifactory.cloud.cms.gov/docker/amazon/aws-cli@sha256:0b894cdaa3836d70050f293b9e993c546e222458e64e145b93a783efd24a7046'
    def image = env.IMAGE_TAG ?: error("IMAGE_TAG not set")

    stage("Cosign Sign") {
        podTemplate(containers: [
            containerTemplate(name: 'cosign', image: cosign_image, command: 'cat', ttyEnabled: true),
            containerTemplate(name: 'aws-cli', image: aws_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                container('cosign') {
                    sh """
                        cosign sign --key awskms:///${kms_key_arn} \
                            --tlog-upload=false \
                            ${image}
                    """
                }
            }
        }
    }
}
