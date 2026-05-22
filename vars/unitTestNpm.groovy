def call(Map config = [:]) {
    def image = config.node_image ?: "artifactory.cloud.cms.gov/docker/node:${config.node_version ?: '20'}"

    stage("npm Test") {
        podTemplate(containers: [
            containerTemplate(name: 'node', image: image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                unstash "workspace"
                container('node') {
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
