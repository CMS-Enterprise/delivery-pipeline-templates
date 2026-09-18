def call(Map config = [:]) {
    def dockerfile = config.dockerfile ?: 'Dockerfile'
    def working_dir = config.working_dir ?: '.'
    def fail_on_error = config.fail_on_error != false
    def trusted_registries = config.trusted_registries ?: 'artifactory.cloud.cms.gov'
    def output_name = config.output_name ?: 'hadolint'
    def stagename = config.stage ?: 'Hadolint Dockerfile Lint'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/hadolint.yaml')) {
            node(POD_LABEL) {
                unstash config.unstash ?: 'workspace'
                container('hadolint') {
                    def exit_code = sh(
                        script: """
                            hadolint ${working_dir}/${dockerfile} \
                                --format json \
                                --trusted-registry ${trusted_registries} \
                                > ${output_name}-results.json
                        """,
                        returnStatus: true
                    )
                    if (exit_code != 0 && fail_on_error) {
                        error "Hadolint found Dockerfile issues in ${working_dir}/${dockerfile}"
                    }
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "${output_name}-results.json"
            }
        }
    }
}
