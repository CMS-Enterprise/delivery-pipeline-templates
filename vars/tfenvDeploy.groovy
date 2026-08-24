void call(Map config = [:], String environment) {
    stage("Terraform Deploy (${environment})") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/terraform.yaml')) {
            node(POD_LABEL) {
                checkout scm

                container('alpine') {
                    def tf_dir = config.terraform_dir ?: "terraform/environments/${environment}"
                    def tf_version = config.terraform_version ?: ""
                    def freshclam_mirror = config.freshclam_mirror ?: "https://artifactory.cloud.cms.gov/clamav-db"

                    sh """
                        apk add --no-cache git bash curl unzip clamav clamav-libunrar
                        freshclam --quiet --DatabaseMirror=${freshclam_mirror}

                        git clone --depth 1 https://github.com/tfutils/tfenv.git /opt/tfenv
                        clamscan -r --infected /opt/tfenv \
                            || { echo "ClamAV detected malware in tfenv"; exit 1; }

                        export PATH="/opt/tfenv/bin:\$PATH"
                        cd ${tf_dir}
                        ${tf_version ? "tfenv install ${tf_version} && tfenv use ${tf_version}" : 'tfenv install && tfenv use'}

                        clamscan -r --infected ~/.tfenv/versions/ \
                            || { echo "ClamAV detected malware in terraform binary"; exit 1; }

                        terraform --version
                    """
                }

                container('aws-cli') {
                    def tf_dir = config.terraform_dir ?: "terraform/environments/${environment}"
                    def var_file = config.var_files?."${environment}" ?: "${environment}.tfvars"
                    def backend_config = config.backend_config?."${environment}" ?: ""
                    def auto_approve = config.auto_approve != null ? config.auto_approve : true

                    dir(tf_dir) {
                        sh """
                            export PATH="/opt/tfenv/bin:\$PATH"
                            terraform init \
                                ${backend_config ? "-backend-config=${backend_config}" : ''} \
                                -input=false
                            terraform plan \
                                -var-file=${var_file} \
                                -out=tfplan \
                                -input=false
                            ${auto_approve ? "terraform apply -input=false tfplan" : ''}
                        """

                        archiveArtifacts allowEmptyArchive: true, artifacts: 'tfplan'
                    }
                }
            }
        }
    }
}
