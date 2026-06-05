def call(Map config = [:]) {
    def test_args = config.test_args ?: '--coverage --ci'

    stage("Jest Test") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/node.yaml')) {
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
