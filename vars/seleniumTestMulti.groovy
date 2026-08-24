void call(Map args = [:]) {
    stage("Selenium Test") {
        def runtime = args.runtime ?: "java"
        def selenium_grid = args.selenium_grid ?: "https://selenium.cloud.cms.gov"

        // The java branch invokes mvn, so it needs the maven image, not gradle.
        def pod_yaml = runtime == "java"
            ? 'resources/pods/maven-openjdk25.yaml'
            : runtime == "python"
                ? 'resources/pods/python.yaml'
                : 'resources/pods/node.yaml'

        def container_name = runtime == "java"
            ? 'maven'
            : runtime == "python"
                ? 'python'
                : 'node'

        podTemplate(yaml: args.pod_yaml ?: readTrusted(pod_yaml)) {
            node(POD_LABEL) {
                unstash "workspace"
                container(container_name) {
                    def base_url = args.base_url ?: env.DEPLOY_URL
                    def browser = args.browser ?: "chrome"

                    withEnv([
                        "SELENIUM_REMOTE_URL=${selenium_grid}/wd/hub",
                        "BROWSER=${browser}",
                        "BASE_URL=${base_url}"
                    ]) {
                        if (runtime == "java") {
                            sh "mvn test -Dselenium.remote.url=${selenium_grid}/wd/hub -Dbrowser=${browser} ${args.maven_args ?: ''}"
                        } else if (runtime == "python") {
                            sh """
                                pip install -r ${args.requirements_file ?: 'requirements.txt'} --quiet
                                pytest ${args.test_path ?: 'tests/selenium/'} \
                                    --junitxml=selenium-results.xml \
                                    ${args.pytest_args ?: ''}
                            """
                        } else {
                            sh """
                                npx wdio run ${args.wdio_config ?: 'wdio.conf.js'} \
                                    ${args.wdio_args ?: ''}
                            """
                        }
                    }
                }
                junit allowEmptyResults: true, testResults: args.results_pattern ?: "**/selenium-results.xml,**/target/surefire-reports/*.xml"
                archiveArtifacts allowEmptyArchive: true, artifacts: "**/screenshots/**"
            }
        }
    }
}
