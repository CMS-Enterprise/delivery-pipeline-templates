def call(Map config = [:]) {
    def severity_threshold = config.severity_threshold ?: 'high'
    def org = config.org ?: env.SNYK_ORG
    def project_name = config.project_name ?: env.REPO_NAME

    stage("Snyk Code Scan") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/snyk.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('snyk') {
                    withCredentials([string(credentialsId: config.token_credential ?: 'snyk-api-token', variable: 'SNYK_TOKEN')]) {
                        sh """
                            snyk auth \$SNYK_TOKEN
                            snyk code test \
                                --severity-threshold=${severity_threshold} \
                                --org=${org} \
                                --project-name=${project_name} \
                                --sarif-file-output=snyk-code-results.sarif
                        """
                    }
                    archiveArtifacts allowEmptyArchive: true, artifacts: "snyk-code-results.sarif"
                }
            }
        }
    }
}
