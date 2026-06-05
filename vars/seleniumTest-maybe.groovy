void call(Map args = [:]) {
    stage("Selenium Test") {
        def runtime = config.runtime ?: "java"
        def selenium_grid = config.selenium_grid ?: "https://selenium.cloud.cms.gov"

        podTemplate(containers: [
            containerTemplate(
                name: 'selenium-runner',
                image: config.runner_image ?: (runtime == "java"
                    ? "artifactory.cloud.cms.gov/docker/maven@sha256:a962f4acb990a9b38a9871ded777b748521e147c7c3eaf8521c98407720edfdb"
                    : runtime == "python"
                        ? "artifactory.cloud.cms.gov/docker/python:${config.python_version ?: '3.12'}"
                        : "artifactory.cloud.cms.gov/docker/node:${config.node_version ?: '20'}"),
                command: 'cat',
                ttyEnabled: true
            )
        ]) {
            node(POD_LABEL) {
                unstash "workspace"
                container('selenium-runner') {
                    def base_url = args.base_url ?: config.base_url ?: env.DEPLOY_URL
                    def browser = config.browser ?: "chrome"

                    withEnv([
                        "SELENIUM_REMOTE_URL=${selenium_grid}/wd/hub",
                        "BROWSER=${browser}",
                        "BASE_URL=${base_url}"
                    ]) {
                        if (runtime == "java") {
                            sh "mvn test -Dselenium.remote.url=${selenium_grid}/wd/hub -Dbrowser=${browser} ${config.maven_args ?: ''}"
                        } else if (runtime == "python") {
                            sh """
                                pip install -r ${config.requirements_file ?: 'requirements.txt'} --quiet
                                pytest ${config.test_path ?: 'tests/selenium/'} \
                                    --junitxml=selenium-results.xml \
                                    ${config.pytest_args ?: ''}
                            """
                        } else {
                            sh """
                                npx wdio run ${config.wdio_config ?: 'wdio.conf.js'} \
                                    ${config.wdio_args ?: ''}
                            """
                        }
                    }
                }
                junit allowEmptyResults: true, testResults: config.results_pattern ?: "**/selenium-results.xml,**/target/surefire-reports/*.xml"
                archiveArtifacts allowEmptyArchive: true, artifacts: "**/screenshots/**"
            }
        }
    }
}
