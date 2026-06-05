def call(Map config = [:], String environment) {
    def tf_version = config.tf_version ?: ''
    def terraform_dir = config.terraform_dir ?: 'terraform/environments'
    def auto_approve = config.auto_approve ?: false
    def var_file = config.var_file ?: "${environment}.tfvars"

    stage("Terraform Deploy (${environment})") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/terraform.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('alpine') {
                    def freshclam_mirror = config.freshclam_mirror ?: "https://artifactory.cloud.cms.gov/clamav-db"
                    sh """
                        apk add --no-cache git bash curl unzip clamav clamav-libunrar
                        freshclam --quiet --DatabaseMirror=${freshclam_mirror}

                        git clone --depth 1 https://github.com/tfutils/tfenv.git /opt/tfenv
                        clamscan -r --infected /opt/tfenv
                        if [ \$? -eq 1 ]; then echo "ClamAV detected malware in tfenv" && exit 1; fi

                        export PATH="/opt/tfenv/bin:\$PATH"
                        cd ${terraform_dir}/${environment}
                        ${tf_version ? "tfenv install ${tf_version} && tfenv use ${tf_version}" : 'tfenv install && tfenv use'}

                        clamscan -r --infected ~/.tfenv/versions/ 2>/dev/null || true
                    """
                }
                container('aws-cli') {
                    dir("${terraform_dir}/${environment}") {
                        withCredentials([string(credentialsId: config.token_credential ?: 'terraform-token', variable: 'TF_TOKEN')]) {
                            sh """
                                export PATH="/opt/tfenv/bin:\$PATH"
                                export TF_TOKEN_app_terraform_io=\$TF_TOKEN

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
