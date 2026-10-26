def call(Map config = [:]) {
    def scan_path = config.scan_path ?: '.'
    def fail_on_secret = config.fail_on_secret != false

    stage("Security Scan: Secrets") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/trufflehog.yaml')) {
            node(POD_LABEL) {
                unstash "workspace"
                container('trufflehog') {
                    sh "cp /opt/trufflehog/trufflehog /home/jenkins/agent/trufflehog"
                    def exit_code = sh (
                        script: "/home/jenkins/agent/trufflehog filesystem ${scan_path} --json ${fail_on_secret ? '--fail' : ''} > trufflehog-results.json",
                        returnStatus: true
                    )
                    if (exit_code != 0 && fail_on_secret) {
                        sh "cat trufflehog-results.json"
                        archiveArtifacts allowEmptyArchive: true, artifacts: "trufflehog-results.json"
                        error "TruffleHog found secrets"
                    }
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "trufflehog-results.json"
            }
        }
    }
}
