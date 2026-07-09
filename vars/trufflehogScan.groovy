def call(Map config = [:]) {
    def scan_path = config.scan_path ?: '.'
    def fail_on_secret = config.fail_on_secret != false

    stage("TruffleHog Secret Scan") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/trufflehog.yaml')) {
            node(POD_LABEL) {
                unstash "workspace"
                container('trufflehog') {
                    def exit_code = sh (
                        script: "/opt/trufflehog/trufflehog filesystem ${scan_path} --json ${fail_on_secret ? '--fail' : ''} > trufflehog-results.json",
                        returnStatus: true
                    )
                    if (exit_code != 0 && fail_on_secret) {
                        sh "cat trufflehog-results.json"
                        archiveArtifacts allowEmptyArchive: true, artifacts: "${scan_path}/trufflehog-results.json"
                        error "TruffleHog found secrets"
                    }
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "${scan_path}/trufflehog-results.json"
            }
        }
    }
}
