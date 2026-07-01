def call(Map config = [:]) {
    def config_file = config.config_file ?: 'config/checkstyle/checkstyle.xml'
    def working_dir = config.working_dir ?: '.'
    def fail_on_violation = config.fail_on_violation != false

    stage("Checkstyle Lint") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/gradle.yaml')) {
            node(POD_LABEL) {
                unstash "workspace"
                container('java') {
                    XXX download jar
                    sh "cd ${working_dir} && java -jar checkstyle*.jar
                }
                recordIssues(
                    tools: [checkStyle(pattern: '**/build/reports/checkstyle/*.xml')],
                    qualityGates: [[threshold: 1, type: 'TOTAL', unstable: !fail_on_violation]]
                )
            }
        }
    }
}
