def call(Map config = [:]) {
    def dockerfile = config.dockerfile ?: 'Dockerfile'
    def fail_on_error = config.fail_on_error != false
    def trusted_registries = config.trusted_registries ?: 'artifactory.cloud.cms.gov'

    stage("Hadolint Dockerfile Lint") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/hadolint.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('hadolint') {
                    def exit_code = sh(
                        script: """
                            hadolint ${dockerfile} \
                                --format json \
                                --trusted-registry ${trusted_registries} \
                                > hadolint-results.json
                        """,
                        returnStatus: true
                    )
                    if (exit_code != 0 && fail_on_error) {
                        error "Hadolint found Dockerfile issues"
                    }
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "hadolint-results.json"
            }
        }
    }
}
