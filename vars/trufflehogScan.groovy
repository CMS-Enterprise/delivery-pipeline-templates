def call(Map config = [:]) {
    def trufflehog_image = config.trufflehog_image ?: 'artifactory.cloud.cms.gov/docker/trufflesecurity/trufflehog@sha256:a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2'
    def scan_path = config.scan_path ?: '.'
    def fail_on_secret = config.fail_on_secret != false

    stage("TruffleHog Secret Scan") {
        podTemplate(containers: [
            containerTemplate(name: 'trufflehog', image: trufflehog_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                unstash "workspace"
                container('trufflehog') {
                    sh """
                        trufflehog filesystem ${scan_path} \
                            --json \
                            ${fail_on_secret ? '--fail' : ''} \
                            | tee trufflehog-results.json
                    """
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "trufflehog-results.json"
            }
        }
    }
}
