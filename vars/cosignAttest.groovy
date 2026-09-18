def call(Map config = [:]) {
    def kms_key_arn = config.kms_key_arn
    if (!kms_key_arn) {
        echo 'cosignAttest: kms_key_arn not set, skipping SBOM attestation'
        return
    }
    def image = config.image ?: env.IMAGE_TAG ?: error('IMAGE_TAG not set')
    def sbom_file = config.sbom_file ?: error('sbom_file is required')
    def predicate_type = config.predicate_type ?: 'cyclonedx'
    def stagename = config.stage ?: 'Cosign Attest SBOM'

    stage("${stagename}") {
        if (!env.AWS_ACCESS_KEY_ID) {
            def account_id = config.account_id
            if (!account_id) {
                echo 'cosignAttest: account_id not set and no AWS credentials, skipping SBOM attestation'
                return
            }
            def aws_env = config.aws_environment ?: 'build'
            awsAssumeRole([
                account_ids: [(aws_env): account_id],
                role_name: config.role_name ?: 'deploy-role',
                region: config.region ?: 'us-east-1',
                pod_yaml: config.aws_pod_yaml,
            ], aws_env)
        }
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/cosign.yaml')) {
            node(POD_LABEL) {
                unstash config.unstash ?: 'workspace'
                container('cosign') {
                    sh """
                        cosign attest --key awskms:///${kms_key_arn} \
                            --predicate ${sbom_file} \
                            --type ${predicate_type} \
                            --tlog-upload=false \
                            ${image}
                    """
                }
            }
        }
    }
}
