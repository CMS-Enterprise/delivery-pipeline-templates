void call(String environment) {
    stage("Terraform Deploy (${environment})") {
        podTemplate(
            serviceAccount: config.service_account ?: 'jenkins-role',
            containers: [
                containerTemplate(
                    name: 'terraform',
                    image: config.terraform_image ?: 'artifactory.cloud.cms.gov/docker/alpine@sha256:4d889c14e7d5a73929ab00be2ef8ff22437e7cbc545931e52554a7b00e123d8b',
                    command: 'cat',
                    ttyEnabled: true
                ),
                containerTemplate(
                    name: 'aws-cli',
                    image: config.aws_image ?: 'artifactory.cloud.cms.gov/docker/amazon/aws-cli@sha256:0b894cdaa3836d70050f293b9e993c546e222458e64e145b93a783efd24a7046',
                    command: 'cat',
                    ttyEnabled: true
                )
            ]
        ) {
            node(POD_LABEL) {
                checkout scm

                container('terraform') {
                    def tf_dir = config.terraform_dir ?: "terraform/environments/${environment}"
                    def tf_version = config.terraform_version ?: ""
                    def freshclam_mirror = config.freshclam_mirror ?: "https://artifactory.cloud.cms.gov/clamav-db"

                    sh """
                        apk add --no-cache clamav clamav-libunrar
                        freshclam --quiet --DatabaseMirror=${freshclam_mirror}

                        git clone https://github.com/tfutils/tfenv.git ~/.tfenv && cd ~/.tfenv && git checkout de6ce2e809c155cbc5e2cfeb3b1bef151244e045 && cd -
                        clamscan -r --infected ~/.tfenv
                        if [ \$? -eq 1 ]; then echo "ClamAV detected malware in tfenv" && exit 1; fi

                        export PATH="\$HOME/.tfenv/bin:\$PATH"
                        cd ${tf_dir}
                        ${tf_version ? "tfenv install ${tf_version} && tfenv use ${tf_version}" : 'tfenv install && tfenv use'}

                        clamscan -r --infected ~/.tfenv/versions/
                        if [ \$? -eq 1 ]; then echo "ClamAV detected malware in terraform binary" && exit 1; fi

                        terraform --version
                    """
                }

                assume_role(environment) {
                    container('terraform') {
                        def tf_dir = config.terraform_dir ?: "terraform/environments/${environment}"
                        def var_file = config.var_files?."${environment}" ?: "${environment}.tfvars"
                        def backend_config = config.backend_config?."${environment}" ?: ""
                        def auto_approve = config.auto_approve != null ? config.auto_approve : true

                        sh """
                            export PATH="\$HOME/.tfenv/bin:\$PATH"
                            cd ${tf_dir}
                            terraform init \
                                ${backend_config ? "-backend-config=${backend_config}" : ''} \
                                -input=false
                            terraform plan \
                                -var-file=${var_file} \
                                -out=tfplan \
                                -input=false
                            ${auto_approve ? "terraform apply -input=false tfplan" : ''}
                        """

                        archiveArtifacts allowEmptyArchive: true, artifacts: "${tf_dir}/tfplan"
                    }
                }
            }
        }
    }
}
