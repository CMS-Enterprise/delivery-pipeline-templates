def call(Map config = [:]) {
    def config_file = config.config_file ?: 'config/checkstyle/checkstyle.xml'
    def fail_on_violation = config.fail_on_violation != false

    stage("Checkstyle Lint") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/gradle.yaml')) {
            node(POD_LABEL) {
                container('gradle') {
                    sh "./gradlew checkstyleMain checkstyleTest --no-daemon"
                }
                recordIssues(
                    tools: [checkStyle(pattern: '**/build/reports/checkstyle/*.xml')],
                    qualityGates: [[threshold: 1, type: 'TOTAL', unstable: !fail_on_violation]]
                )
            }
        }
    }
}
