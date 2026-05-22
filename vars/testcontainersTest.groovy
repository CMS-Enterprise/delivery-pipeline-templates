def call(Map config = [:]) {
    def gradle_image = config.gradle_image ?: 'artifactory.cloud.cms.gov/docker/gradle@sha256:a39ba51afef66ce9ea170c2df9d303cb8cb8619be0b5afddfe06696b5327b775'
    def test_task = config.test_task ?: 'integrationTest'

    stage("Testcontainers Integration Test") {
        podTemplate(containers: [
            containerTemplate(name: 'gradle', image: gradle_image, command: 'cat', ttyEnabled: true, privileged: true)
        ]) {
            node(POD_LABEL) {
                unstash "workspace"
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
