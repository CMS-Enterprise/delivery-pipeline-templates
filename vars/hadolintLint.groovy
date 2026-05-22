def call(Map config = [:]) {
    def hadolint_image = config.hadolint_image ?: 'artifactory.cloud.cms.gov/docker/hadolint/hadolint@sha256:c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3'
    def dockerfile = config.dockerfile ?: 'Dockerfile'
    def fail_on_error = config.fail_on_error != false
    def trusted_registries = config.trusted_registries ?: 'artifactory.cloud.cms.gov'

    stage("Hadolint Dockerfile Lint") {
        podTemplate(containers: [
            containerTemplate(name: 'hadolint', image: hadolint_image, command: 'cat', ttyEnabled: true)
        ]) {
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
