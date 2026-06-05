def call(Map config = [:], String environment) {
    def stack_name = config.stack_name ?: "${env.REPO_NAME}-${environment}"
    def template_file = config.template_file ?: 'cloudformation/template.yaml'
    def parameters_file = config.parameters_file ?: "cloudformation/params-${environment}.json"
    def region = config.region ?: 'us-east-1'
    def capabilities = config.capabilities ?: 'CAPABILITY_IAM CAPABILITY_NAMED_IAM'

    stage("CloudFormation Deploy (${environment})") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/aws-cli.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('aws-cli') {
                    sh """
                        aws cloudformation deploy \
                            --stack-name ${stack_name} \
                            --template-file ${template_file} \
                            --parameter-overrides file://${parameters_file} \
                            --capabilities ${capabilities} \
                            --region ${region} \
                            --no-fail-on-empty-changeset
                    """
                    sh """
                        aws cloudformation wait stack-update-complete \
                            --stack-name ${stack_name} \
                            --region ${region} || true
                    """
                }
            }
        }
    }
}
