def call(Map config = [:]) {
    def task = config.test_task ?: 'test'

    stage("Gradle Test") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/gradle.yaml')) {
            node(POD_LABEL) {
                unstash "workspace"
                container('gradle') {
                    sh "./gradlew ${task} --no-daemon"
                }
                junit allowEmptyResults: true, testResults: '**/build/test-results/**/*.xml'
            }
        }
    }
}
