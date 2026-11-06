def call(Map config = [:]) {
    def test_args = config.test_args ?: '--coverage --ci'
    def working_dir = config.working_dir ?: '.'
    def myunstash = config.unstash ?: 'workspace'
    def stagename = config.stage ?: 'Jest Test'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/node.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                def status = 0
                container('node') {
                    status = sh(returnStatus: true,
                        script: "cd ${working_dir} && npx jest ${test_args} --reporters=default --reporters=jest-junit")
                }
                junit allowEmptyResults: true, testResults: "${working_dir}/junit.xml"
                // archiveArtifacts, not publishHTML: the HTML Publisher plugin is
                // not installed on the controller, so publishHTML throws
                // NoSuchMethodError and fails the stage after the tests pass.
                archiveArtifacts allowEmptyArchive: true, artifacts: "${working_dir}/coverage/**"
                if (status != 0) {
                    error "Jest tests failed (exit ${status})"
                }
            }
        }
    }
}
