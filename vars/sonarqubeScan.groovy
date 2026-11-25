def call(Map config = [:]) {
    def project_key = config.project_key ?: env.REPO_NAME
    def sonar_url = config.url ?: 'https://sonarqube.cloud.cms.gov'
    def source_path = config.source_path ?: '.'
    def exclusions = config.exclusions ?: ''
    def quality_gate_wait = config.quality_gate_wait != false
    // MB for the JS/TS analyzer's Node.js heap. Left unset it scales to ~55% of
    // the container limit, which with the scanner JVM overruns it (OOMKilled).
    def node_maxspace = config.node_maxspace ?: 2048

    stage("SonarQube Analysis") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/sonar-scanner.yaml')) {
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
                                -Dsonar.javascript.node.maxspace=${node_maxspace} \
                                -Dsonar.qualitygate.wait=${quality_gate_wait}
                        """
                    }
                }
            }
        }
    }
}
