def call(Map config = [:]) {
    stage("npm Test") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/node.yaml')) {
            node(POD_LABEL) {
                unstash "workspace"
                container('node') {
                    sh "npm config set registry https://artifactory.cloud.cms.gov/artifactory/api/npm/npm/"
                    sh "npm test -- --coverage"
                }
                publishHTML(target: [
                    reportDir: "coverage/lcov-report",
                    reportFiles: "index.html",
                    reportName: "Coverage Report"
                ])
            }
        }
    }
}
