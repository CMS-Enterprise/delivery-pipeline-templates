def call(Map config = [:], String environment) {
    def role_name = config.role_name ?: 'deploy-role'
    def region = config.region ?: 'us-east-1'
    def account_id = config.account_ids?."${environment}" ?: error("No account_id configured for ${environment}")
    def role_arn = "arn:aws:iam::${account_id}:role/${role_name}"
    def aws_image = config.aws_image ?: 'artifactory.cloud.cms.gov/docker/amazon/aws-cli@sha256:0b894cdaa3836d70050f293b9e993c546e222458e64e145b93a783efd24a7046'

    stage("Assume Role (${environment})") {
        podTemplate(containers: [
            containerTemplate(name: 'aws-cli', image: aws_image, command: 'cat', ttyEnabled: true)
        ]) {
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
