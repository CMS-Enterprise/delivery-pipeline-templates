def call(Map config = [:]) {
    def task = config.test_task ?: 'cucumber'
    def tags = config.tags ?: ''
    def working_dir = config.working_dir ?: '.'
    def myunstash = config.unstash ?: 'workspace'
    def stagename = config.stage ?: 'Cucumber Test'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/gradle.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                def status = 0
                container('gradle') {
                    status = sh(returnStatus: true, script: """
                        cd ${working_dir}
                        gradle ${task} --no-daemon --console=plain ${tags ? "-Dcucumber.filter.tags='${tags}'" : ''}
                    """)
                }
                // junit, not the cucumber publisher: the Cucumber Reports plugin
                // is not installed on the controller.
                junit allowEmptyResults: true, testResults: "${working_dir}/build/test-results/${task}/*.xml"
                archiveArtifacts allowEmptyArchive: true, artifacts: "${working_dir}/build/reports/cucumber/**"
                if (status != 0) {
                    error "Cucumber scenarios failed (exit ${status})"
                }
            }
        }
    }
}
