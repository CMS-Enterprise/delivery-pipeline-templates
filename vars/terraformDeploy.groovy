// Terraform runs from the pinned IronBank image in the pod, so no binary is
// downloaded at run time; the code's required_version constrains it. Providers
// fetched by `terraform init` must match the committed .terraform.lock.hcl and
// are ClamAV-scanned before plan. auto_approve: false stops after plan.
void call(Map config = [:], String environment) {
    def tf_dir = config.terraform_dir ?: "terraform/environments/${environment}"
    def var_file = config.var_files?."${environment}" ?: "${environment}.tfvars"
    def backend_config = config.backend_config?."${environment}" ?: ''
    def auto_approve = config.auto_approve != null ? config.auto_approve : false
    def token_credential = config.token_credential ?: ''
    def myunstash = config.unstash ?: ''
    def stagename = config.stage ?: "Terraform Deploy (${environment})"

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/terraform.yaml')) {
            node(POD_LABEL) {
                if (myunstash) {
                    unstash "${myunstash}"
                } else {
                    checkout scm
                }
                // Terraform Cloud/Enterprise token, only for code that uses a
                // remote backend or private registry on app.terraform.io.
                def withToken = { Closure body ->
                    if (token_credential) {
                        withCredentials([string(credentialsId: token_credential, variable: 'TF_TOKEN_app_terraform_io')]) {
                            body()
                        }
                    } else {
                        body()
                    }
                }
                dir(tf_dir) {
                    container('terraform') {
                        withToken {
                            sh """
                                terraform version
                                terraform init -input=false -lockfile=readonly \
                                    ${backend_config ? "-backend-config=${backend_config}" : ''}
                            """
                        }
                    }
                    container('clamav') {
                        sh """
                            freshclam
                            clamscan --infected --recursive .terraform/providers
                        """
                    }
                    container('terraform') {
                        withToken {
                            sh "terraform plan -input=false -var-file=${var_file} -out=tfplan"
                            if (auto_approve) {
                                sh 'terraform apply -input=false tfplan'
                            }
                        }
                    }
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'tfplan'
                }
            }
        }
    }
}
