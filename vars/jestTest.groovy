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
                // archiveArtifacts, not publishHTML: the HTML Publisher plugin is
                // not installed on the controller, so publishHTML throws
                // NoSuchMethodError and fails the stage after the tests pass.
                archiveArtifacts allowEmptyArchive: true, artifacts: 'coverage/**'
            }
        }
    }
}
