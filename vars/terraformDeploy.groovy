def call(Map config = [:], String environment) {
    def base_image = config.base_image ?: 'artifactory.cloud.cms.gov/docker/alpine:3'
    def tfenv_repo = config.tfenv_repo ?: 'https://github.com/tfutils/tfenv.git'
    def tf_version = config.tf_version ?: ''
    def terraform_dir = config.terraform_dir ?: 'terraform/environments'
    def auto_approve = config.auto_approve ?: false
    def var_file = config.var_file ?: "${environment}.tfvars"

    stage("Terraform Deploy (${environment})") {
        podTemplate(containers: [
            containerTemplate(name: 'tfenv', image: base_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                checkout scm
                container('tfenv') {
                    withCredentials([string(credentialsId: config.token_credential ?: 'terraform-token', variable: 'TF_TOKEN')]) {
                        sh """
                            apk add --no-cache git bash curl unzip
                            git clone --depth 1 ${tfenv_repo} /opt/tfenv
                            export PATH="/opt/tfenv/bin:\$PATH"
                            ln -s /opt/tfenv/bin/* /usr/local/bin/ 2>/dev/null || true
                        """

                        dir("${terraform_dir}/${environment}") {
                            sh """
                                export PATH="/opt/tfenv/bin:\$PATH"
                                export TF_TOKEN_app_terraform_io=\$TF_TOKEN

                                ${tf_version ? "tfenv install ${tf_version} && tfenv use ${tf_version}" : 'tfenv install && tfenv use'}

                                terraform --version
                                terraform init -input=false
                                terraform plan -var-file=${var_file} -out=tfplan
                                ${auto_approve ? 'terraform apply -auto-approve tfplan' : ''}
                            """
                            if (!auto_approve) {
                                input "Apply Terraform changes to ${environment}?"
                                sh """
                                    export PATH="/opt/tfenv/bin:\$PATH"
                                    export TF_TOKEN_app_terraform_io=\$TF_TOKEN
                                    terraform apply tfplan
                                """
                            }
                        }
                    }
                }
            }
        }
    }
}
