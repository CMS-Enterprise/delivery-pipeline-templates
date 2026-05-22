def call(Map config = [:]) {
    def base_image = config.base_image ?: 'artifactory.cloud.cms.gov/docker/alpine:3'
    def tfenv_repo = config.tfenv_repo ?: 'https://github.com/tfutils/tfenv.git'
    def tf_version = config.tf_version ?: ''
    def tflint_version = config.tflint_version ?: 'latest'
    def terraform_dir = config.terraform_dir ?: 'terraform/'
    def fail_on_error = config.fail_on_error != false
    def minimum_severity = config.minimum_severity ?: 'warning'

    stage("TFLint") {
        podTemplate(containers: [
            containerTemplate(name: 'tfenv', image: base_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                checkout scm
                container('tfenv') {
                    sh """
                        apk add --no-cache git bash curl unzip jq

                        # Install tfenv + terraform
                        git clone --depth 1 ${tfenv_repo} /opt/tfenv
                        export PATH="/opt/tfenv/bin:\$PATH"
                        ln -s /opt/tfenv/bin/* /usr/local/bin/ 2>/dev/null || true
                    """

                    dir(terraform_dir) {
                        sh """
                            export PATH="/opt/tfenv/bin:\$PATH"
                            ${tf_version ? "tfenv install ${tf_version} && tfenv use ${tf_version}" : 'tfenv install && tfenv use'}
                            terraform --version
                        """

                        // Install tflint
                        sh """
                            if [ "${tflint_version}" = "latest" ]; then
                                TFLINT_VERSION=\$(curl -s https://api.github.com/repos/terraform-linters/tflint/releases/latest | jq -r .tag_name)
                            else
                                TFLINT_VERSION="v${tflint_version}"
                            fi
                            curl -sSL "https://github.com/terraform-linters/tflint/releases/download/\${TFLINT_VERSION}/tflint_linux_amd64.zip" -o tflint.zip
                            unzip -o tflint.zip -d /usr/local/bin/
                            rm tflint.zip
                            tflint --version
                        """

                        def exit_code = sh(
                            script: """
                                export PATH="/opt/tfenv/bin:\$PATH"
                                tflint --init
                                tflint \
                                    --minimum-failure-severity=${minimum_severity} \
                                    --format=json > tflint-results.json
                            """,
                            returnStatus: true
                        )
                        if (exit_code != 0 && fail_on_error) {
                            error "TFLint found issues"
                        }
                    }
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "**/tflint-results.json"
            }
        }
    }
}
