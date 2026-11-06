// Terraform comes from the pinned image in the pod rather than a tfenv
// download, so no unscanned binary is fetched at run time. tfenv's
// .terraform-version file is still honored: the step fails if the image's
// terraform does not match it. Providers fetched by `terraform init` must match
// the committed .terraform.lock.hcl and are ClamAV-scanned before plan.
void call(Map config = [:], String environment) {
    def tf_dir = config.terraform_dir ?: "terraform/environments/${environment}"
    def var_file = config.var_files?."${environment}" ?: "${environment}.tfvars"
    def backend_config = config.backend_config?."${environment}" ?: ''
    def auto_approve = config.auto_approve != null ? config.auto_approve : true
    def myunstash = config.unstash ?: ''
    def stagename = config.stage ?: "Terraform Deploy (${environment})"

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/terraform-scan.yaml')) {
            node(POD_LABEL) {
                if (myunstash) {
                    unstash "${myunstash}"
                } else {
                    checkout scm
                }
                dir(tf_dir) {
                    container('terraform') {
                        sh """
                            set -e
                            if [ -f .terraform-version ]; then
                                want=\$(tr -d '[:space:]' < .terraform-version)
                                have=\$(terraform version -json | sed -n 's/.*"terraform_version": *"\\([^"]*\\)".*/\\1/p')
                                if [ "\$want" != "\$have" ]; then
                                    echo ".terraform-version wants \$want but the pod image provides \$have"
                                    exit 1
                                fi
                            fi
                            terraform init -input=false -lockfile=readonly \
                                ${backend_config ? "-backend-config=${backend_config}" : ''}
                        """
                    }
                    container('clamav') {
                        sh """
                            freshclam
                            clamscan --infected --recursive .terraform/providers
                        """
                    }
                    container('terraform') {
                        sh "terraform plan -input=false -var-file=${var_file} -out=tfplan"
                        if (auto_approve) {
                            sh 'terraform apply -input=false tfplan'
                        }
                    }
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'tfplan'
                }
            }
        }
    }
}
