def call(Map config = [:]) {
    def task = config.test_task ?: 'test'
    def working_dir = config.working_dir ?: '.'
    def stagename = config.stage ?: 'Gradle Test'
    def myunstash = config.unstash ?: 'workspace'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/gradle.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                container('gradle') {
                    sh "cd ${working_dir} && gradle ${task} --no-daemon --console=plain"
                }
                junit allowEmptyResults: true, testResults: "${working_dir}/**/build/test-results/**/*.xml"
            }
        }
    }
}
