def call(Map config = [:]) {
    def snyk_image = config.snyk_image ?: 'artifactory.cloud.cms.gov/docker/snyk/snyk@sha256:9d890769442afa9bd185403e7c199a60ea70b6784865e9ed0df2380f45e1c028'
    def severity_threshold = config.severity_threshold ?: 'high'
    def org = config.org ?: env.SNYK_ORG
    def project_name = config.project_name ?: env.REPO_NAME

    stage("Snyk Code Scan") {
        podTemplate(containers: [
            containerTemplate(name: 'snyk', image: snyk_image, command: 'cat', ttyEnabled: true)
        ]) {
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
