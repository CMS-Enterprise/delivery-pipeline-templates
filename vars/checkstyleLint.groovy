def call(Map config = [:]) {
    def gradle_image = config.gradle_image ?: 'artifactory.cloud.cms.gov/docker/gradle@sha256:a39ba51afef66ce9ea170c2df9d303cb8cb8619be0b5afddfe06696b5327b775'
    def config_file = config.config_file ?: 'config/checkstyle/checkstyle.xml'
    def fail_on_violation = config.fail_on_violation != false

    stage("Checkstyle Lint") {
        podTemplate(containers: [
            containerTemplate(name: 'gradle', image: gradle_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                unstash "workspace"
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
