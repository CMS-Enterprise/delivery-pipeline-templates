def call(Map config = [:]) {
    def image = config.gradle_image ?: 'artifactory.cloud.cms.gov/docker/gradle@sha256:a39ba51afef66ce9ea170c2df9d303cb8cb8619be0b5afddfe06696b5327b775'
    def task = config.test_task ?: 'test'

    stage("Gradle Test") {
        podTemplate(containers: [
            containerTemplate(name: 'gradle', image: image, command: 'cat', ttyEnabled: true)
        ]) {
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
