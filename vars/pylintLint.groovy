def call(Map config = [:]) {
    def python_image = config.python_image ?: "artifactory.cloud.cms.gov/docker/python:${config.python_version ?: '3.12'}"
    def paths = config.paths ?: 'src/'
    def min_score = config.min_score ?: '7.0'
    def requirements_file = config.requirements_file ?: 'requirements-dev.txt'

    stage("Pylint") {
        podTemplate(containers: [
            containerTemplate(name: 'python', image: python_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                checkout scm
                container('python') {
                    sh """
                        pip install pylint
                        [ -f ${requirements_file} ] && pip install -r ${requirements_file}
                        pylint ${paths} \
                            --output-format=json:pylint-results.json,text \
                            --fail-under=${min_score} || true
                    """
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "pylint-results.json"
            }
        }
    }
}
