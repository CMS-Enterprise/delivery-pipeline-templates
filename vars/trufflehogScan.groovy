def call(Map config = [:]) {
    def scan_path = config.scan_path ?: '.'
    def fail_on_secret = config.fail_on_secret != false

    stage("TruffleHog Secret Scan") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/trufflehog.yaml')) {
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
