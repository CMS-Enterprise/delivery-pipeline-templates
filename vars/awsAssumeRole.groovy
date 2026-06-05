def call(Map config = [:], String environment) {
    def role_name = config.role_name ?: 'deploy-role'
    def region = config.region ?: 'us-east-1'
    def account_id = config.account_ids?."${environment}" ?: error("No account_id configured for ${environment}")
    def role_arn = "arn:aws:iam::${account_id}:role/${role_name}"

    stage("Assume Role (${environment})") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/aws-cli.yaml')) {
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
            }
        }
    }
}
