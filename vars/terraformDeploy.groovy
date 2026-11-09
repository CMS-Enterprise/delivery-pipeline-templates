// Terraform runs from the pinned IronBank image in the pod, so no binary is
// downloaded at run time; the code's required_version constrains it. Providers
// fetched by `terraform init` must match the committed .terraform.lock.hcl and
// are ClamAV-scanned before plan. auto_approve: false stops after plan.
// provider_mirror (a network mirror URL, e.g. an Artifactory Terraform remote's
// .../api/terraform/<repo>/providers/) makes it the only provider source.
void call(Map config = [:], String environment) {
    def tf_dir = config.terraform_dir ?: "terraform/environments/${environment}"
    def var_file = config.var_files?."${environment}" ?: "${environment}.tfvars"
    def backend_config = config.backend_config?."${environment}" ?: ''
    def auto_approve = config.auto_approve != null ? config.auto_approve : false
    def token_credential = config.token_credential ?: ''
    def provider_mirror = config.provider_mirror ?: ''
    if (provider_mirror && !provider_mirror.endsWith('/')) {
        provider_mirror = "${provider_mirror}/"
    }
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
                def cli_env = []
                if (provider_mirror) {
                    def cli_config = "${env.WORKSPACE}/.terraform-cli.tfrc"
                    writeFile file: cli_config, text: """provider_installation {
  network_mirror {
    url = "${provider_mirror}"
  }
}
"""
                    cli_env = ["TF_CLI_CONFIG_FILE=${cli_config}"]
                }
                // Applies the mirror config, plus the Terraform Cloud/Enterprise
                // token for code that uses app.terraform.io backends or modules.
                def withTerraformEnv = { Closure body ->
                    withEnv(cli_env) {
                        if (token_credential) {
                            withCredentials([string(credentialsId: token_credential, variable: 'TF_TOKEN_app_terraform_io')]) {
                                body()
                            }
                        } else {
                            body()
                        }
                    }
                }
                dir(tf_dir) {
                    container('terraform') {
                        withTerraformEnv {
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
                        withTerraformEnv {
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
