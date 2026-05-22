def call(Map config = [:], String environment) {
    def aws_image = config.aws_image ?: 'artifactory.cloud.cms.gov/docker/amazon/aws-cli@sha256:0b894cdaa3836d70050f293b9e993c546e222458e64e145b93a783efd24a7046'
    def stack_name = config.stack_name ?: "${env.REPO_NAME}-${environment}"
    def template_file = config.template_file ?: 'cloudformation/template.yaml'
    def parameters_file = config.parameters_file ?: "cloudformation/params-${environment}.json"
    def region = config.region ?: 'us-east-1'
    def capabilities = config.capabilities ?: 'CAPABILITY_IAM CAPABILITY_NAMED_IAM'

    stage("CloudFormation Deploy (${environment})") {
        podTemplate(containers: [
            containerTemplate(name: 'aws-cli', image: aws_image, command: 'cat', ttyEnabled: true)
        ]) {
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
