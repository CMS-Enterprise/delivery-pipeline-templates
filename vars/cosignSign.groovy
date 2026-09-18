def call(Map config = [:]) {
    def kms_key_arn = config.kms_key_arn
    if (!kms_key_arn) {
        echo 'cosignSign: kms_key_arn not set, skipping image signing'
        return
    }
    def account_id = config.account_id
    def image = config.image ?: env.IMAGE_TAG ?: error('IMAGE_TAG not set')
    def stagename = config.stage ?: 'Cosign Sign'

    stage("${stagename}") {
        if (!env.AWS_ACCESS_KEY_ID) {
            if (!account_id) {
                echo 'cosignSign: account_id not set and no AWS credentials, skipping image signing'
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
