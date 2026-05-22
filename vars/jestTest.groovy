def call(Map config = [:]) {
    def node_image = config.node_image ?: "artifactory.cloud.cms.gov/docker/node:${config.node_version ?: '20'}"
    def test_args = config.test_args ?: '--coverage --ci'

    stage("Jest Test") {
        podTemplate(containers: [
            containerTemplate(name: 'node', image: node_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                unstash "workspace"
                container('node') {
                    sh "npx jest ${test_args} --reporters=default --reporters=jest-junit"
                }
                junit allowEmptyResults: true, testResults: 'junit.xml'
                publishHTML(target: [
                    reportDir: "coverage/lcov-report",
                    reportFiles: "index.html",
                    reportName: "Jest Coverage Report"
                ])
            }
        }
    }
}
