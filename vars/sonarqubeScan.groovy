def call(Map config = [:]) {
    def sonar_image = config.sonar_image ?: 'artifactory.cloud.cms.gov/docker/sonarsource/sonar-scanner-cli@sha256:c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9'
    def project_key = config.project_key ?: env.REPO_NAME
    def sonar_url = config.url ?: 'https://sonarqube.cloud.cms.gov'
    def source_path = config.source_path ?: '.'
    def exclusions = config.exclusions ?: ''
    def quality_gate_wait = config.quality_gate_wait != false

    stage("SonarQube Analysis") {
        podTemplate(containers: [
            containerTemplate(name: 'sonar-scanner', image: sonar_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                unstash "workspace"
                container('sonar-scanner') {
                    withCredentials([string(credentialsId: config.token_credential ?: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                        sh """
                            sonar-scanner \
                                -Dsonar.host.url=${sonar_url} \
                                -Dsonar.token=\$SONAR_TOKEN \
                                -Dsonar.projectKey=${project_key} \
                                -Dsonar.sources=${source_path} \
                                ${exclusions ? "-Dsonar.exclusions=${exclusions}" : ''} \
                                -Dsonar.qualitygate.wait=${quality_gate_wait}
                        """
                    }
                }
            }
        }
    }
}
