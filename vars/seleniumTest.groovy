def call(Map config = [:]) {
    def gradle_image = config.gradle_image ?: 'artifactory.cloud.cms.gov/docker/gradle@sha256:a39ba51afef66ce9ea170c2df9d303cb8cb8619be0b5afddfe06696b5327b775'
    def test_task = config.test_task ?: 'seleniumTest'
    def base_url = config.base_url ?: env.DEPLOY_URL

    stage("Selenium Test") {
        podTemplate(containers: [
            containerTemplate(name: 'gradle', image: gradle_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                unstash "workspace"
                container('gradle') {
                    sh """
                        ./gradlew ${test_task} --no-daemon \
                            -Dselenium.hub=https://seleniumbox.cloud.cms.gov/wd/hub \
                            -Dbase.url=${base_url}
                    """
                }
                junit allowEmptyResults: true, testResults: '**/build/test-results/**/*.xml'
            }
        }
    }
}
