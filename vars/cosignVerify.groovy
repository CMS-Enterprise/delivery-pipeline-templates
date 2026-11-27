def call(Map config = [:]) {
    def kms_key_arn = config.kms_key_arn
    // A public key committed to the repo (path relative to its root) verifies
    // without AWS access; otherwise the KMS key is used.
    def public_key = config.public_key
    if (!kms_key_arn && !public_key) {
        echo "No KMS key ARN or public key provided — skipping cosign verify"
        return
    }
    def image = config.image ?: env.IMAGE_TAG ?: error("image is required (env.IMAGE_TAG is not set)")
    def stagename = config.stage ?: "Cosign Verify"
    def key_ref = public_key ? 'cosign-verify.pub' : "awskms:///${kms_key_arn}"

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/cosign.yaml')) {
            node(POD_LABEL) {
                if (public_key) {
                    // readTrusted takes the key from the trusted revision, so a
                    // change under review cannot substitute its own key.
                    writeFile file: key_ref, text: readTrusted(public_key)
                }
                container('cosign') {
                    registryAuth(config) {
                        sh """
                            cosign verify --key ${key_ref} \
                                --insecure-ignore-tlog=true \
                                ${image} > /dev/null
                        """
                    }
                }
                echo "Cosign signature verified: ${image}"
            }
        }
    }
}
