def call(Map config = [:], String environment) {
    def role_name = config.role_name ?: 'deploy-role'
    def region = config.region ?: 'us-east-1'
    def account_id = config.account_ids?."${environment}" ?: error("No account_id configured for ${environment}")
    def role_arn = "arn:aws:iam::${account_id}:role/${role_name}"

    stage("Assume Role (${environment})") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/aws-cli.yaml')) {
            node(POD_LABEL) {
                container('aws-cli') {
                    def assume = {
                        sh(
                            script: """
                                aws sts assume-role \
                                    --role-arn ${role_arn} \
                                    --role-session-name jenkins-${env.BUILD_NUMBER} \
                                    --region ${region} \
                                    --query '[Credentials.AccessKeyId,Credentials.SecretAccessKey,Credentials.SessionToken]' \
                                    --output text
                            """,
                            returnStdout: true
                        ).trim()
                    }
                    // Without a credential the pod's own identity (e.g. IRSA on
                    // the service account) is used to call sts:AssumeRole.
                    def creds
                    if (config.credential) {
                        withCredentials([usernamePassword(credentialsId: config.credential,
                                usernameVariable: 'AWS_ACCESS_KEY_ID',
                                passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                            creds = assume()
                        }
                    } else {
                        creds = assume()
                    }
                    // Text output rather than JSON: the library runs in the Groovy
                    // sandbox, which rejects JsonSlurperClassic. None of the three
                    // values contain whitespace, so a split is unambiguous.
                    def parts = creds.split(/\s+/)
                    if (parts.size() != 3) {
                        error("Unexpected sts assume-role output: expected 3 fields, got ${parts.size()}")
                    }
                    env.AWS_ACCESS_KEY_ID = parts[0]
                    env.AWS_SECRET_ACCESS_KEY = parts[1]
                    env.AWS_SESSION_TOKEN = parts[2]
                    env.AWS_REGION = region
                }
            }
        }
    }
}
