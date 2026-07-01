def call(Map config = [:]) {
    def test_task = config.test_task ?: 'seleniumTest'
    def base_url = config.base_url ?: env.DEPLOY_URL

    stage("Selenium Test") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/gradle.yaml')) {
            node(POD_LABEL) {
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
