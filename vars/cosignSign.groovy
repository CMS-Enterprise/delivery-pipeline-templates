def call(Map config = [:]) {
    // Fails closed: every published container must carry a signature, so a
    // missing key or account is an error rather than a silent skip.
    def kms_key_arn = config.kms_key_arn ?: error('kms_key_arn is required for cosign signing')
    def account_id = config.account_id ?: error('account_id is required for cosign signing')

    def image = config.image ?: env.IMAGE_TAG ?: error('IMAGE_TAG not set')
    def role_name = config.role_name ?: 'deploy-role'
    def region = config.region ?: 'us-east-1'
    def role_arn = "arn:aws:iam::${account_id}:role/${role_name}"
    def stagename = config.stage ?: 'Cosign Sign'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/cosign.yaml')) {
            node(POD_LABEL) {
                def parsed = null
                container('aws-cli') {
                    def creds = sh(
                        script: """
                            aws sts assume-role \
                                --role-arn ${role_arn} \
                                --role-session-name jenkins-${env.BUILD_NUMBER} \
                                --region ${region} \
                                --output json
                        """,
                        returnStdout: true
                    ).trim()
                    parsed = readJSON text: creds
                }
                // Scoped with withEnv rather than assigned to env.*, which is
                // pipeline-global and would leak across parallel signings.
                withEnv([
                    "AWS_ACCESS_KEY_ID=${parsed.Credentials.AccessKeyId}",
                    "AWS_SECRET_ACCESS_KEY=${parsed.Credentials.SecretAccessKey}",
                    "AWS_SESSION_TOKEN=${parsed.Credentials.SessionToken}"
                ]) {
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
}
