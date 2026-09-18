def call(Map config = [:]) {
    def kms_key_arn = config.kms_key_arn ?: error('kms_key_arn is required for cosign signing')
    def image = config.image ?: env.IMAGE_TAG ?: error('IMAGE_TAG not set')
    def stagename = config.stage ?: 'Cosign Sign'

    stage("${stagename}") {
        if (!env.AWS_ACCESS_KEY_ID) {
            def account_id = config.account_id ?: error('account_id is required when AWS credentials are not already set')
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
