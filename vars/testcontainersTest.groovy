def call(Map config = [:]) {
    def test_task = config.test_task ?: 'integrationTest'

    stage("Testcontainers Integration Test") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/gradle.yaml')) {
            node(POD_LABEL) {
                container('gradle') {
                    sh """
                        export TESTCONTAINERS_RYUK_DISABLED=true
                        ./gradlew ${test_task} --no-daemon
                    """
                }
                junit allowEmptyResults: true, testResults: '**/build/test-results/**/*.xml'
            }
        }
    }
}
