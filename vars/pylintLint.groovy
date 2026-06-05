def call(Map config = [:]) {
    def paths = config.paths ?: 'src/'
    def min_score = config.min_score ?: '7.0'
    def requirements_file = config.requirements_file ?: 'requirements-dev.txt'

    stage("Pylint") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/python.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('python') {
                    sh """
                        pip install pylint
                        [ -f ${requirements_file} ] && pip install -r ${requirements_file}
                        pylint ${paths} \
                            --output-format=json:pylint-results.json,text \
                            --fail-under=${min_score}
                    """
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "pylint-results.json"
            }
        }
    }
}
