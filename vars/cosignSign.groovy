def call(Map config = [:]) {
    def kms_key_arn = config.kms_key_arn
    if (!kms_key_arn) {
        echo "No KMS key ARN provided — skipping cosign sign"
        return
    }

    def role_name = config.role_name ?: 'deploy-role'
    def region = config.region ?: 'us-east-1'
    def account_id = config.account_ids?. error
    def role_arn = "arn:aws:iam::${account_id}:role/${role_name}"

    def cosignYaml = config.cosign_yaml ?: readTrusted 'resources/pods/cosign.yaml'
    def awsYaml = config.aws_yaml ?: readTrusted 'resources/pods/aws-cli.yaml'



    stage("Cosign Sign") {
        podTemplate(yaml: cosignYaml) {
           podTemplate(yaml: awsYaml){
             node(POD_LABEL) {
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
                    def parsed = readJSON text: creds
                    env.AWS_ACCESS_KEY_ID = parsed.Credentials.AccessKeyId
                    env.AWS_SECRET_ACCESS_KEY = parsed.Credentials.SecretAccessKey
                    env.AWS_SESSION_TOKEN = parsed.Credentials.SessionToken
                }
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
