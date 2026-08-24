def call(Map config = [:]) {
    def working_dir = config.working_dir ?: '.'
    def fail_on_violation = config.fail_on_violation != false
    def task = config.task ?: 'checkstyleMain'
    def stagename = config.stage ?: 'Checkstyle Lint'
    def myunstash = config.unstash ?: 'workspace'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/gradle.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                container('gradle') {
                    sh "cd ${working_dir} && ./gradlew ${task} --no-daemon"
                }
                recordIssues(
                    tools: [checkStyle(pattern: "${working_dir}/**/build/reports/checkstyle/*.xml")],
                    qualityGates: [[threshold: 1, type: 'TOTAL', unstable: !fail_on_violation]]
                )
            }
        }
    }
}
