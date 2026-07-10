def call(Map config = [:]) {
    def severity_threshold = config.severity_threshold ?: 'high'
    def org = config.org ?: env.SNYK_ORG
    def monitor = config.monitor ?: false

    stage("Snyk Dependency Scan") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/snyk.yaml')) {
            node(POD_LABEL) {
                unstash workspace
                container('snyk') {
                    withCredentials([string(credentialsId: config.token_credential ?: 'snyk-api-token', variable: 'SNYK_TOKEN')]) {
                        sh """
                            snyk auth \$SNYK_TOKEN
                            snyk test \
                                --severity-threshold=${severity_threshold} \
                                --org=${org} \
                                --json-file-output=snyk-deps-results.json
                        """
                        if (monitor) {
                            sh "snyk monitor --org=${org}"
                        }
                    }
                    archiveArtifacts allowEmptyArchive: true, artifacts: "snyk-deps-results.json"
                }
            }
        }
    }
}
