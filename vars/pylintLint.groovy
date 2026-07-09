def call(Map config = [:]) {
    def paths = config.paths ?: 'src/'
    def working_dir = config.working_dir ?: '.'
    def min_score = config.min_score ?: '7.0'
    def requirements_file = config.requirements_file ?: 'requirements.txt'

    stage("Lint: Python") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/python.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('python') {
                    sh """
                        pip3.14 config set global.index-url https://artifactory.cloud.cms.gov/artifactory/api/pypi/python/simple
                        cd ${working_dir}
                        pip3.14 install pylint pylint-django
                        [ -f ${requirements_file} ] && pip3.14 install -r ${requirements_file}
                        pylint ${paths} \
                            --output-format=json:pylint-results.json,text \
                            --fail-under=${min_score}
                    """
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "${working_dir}/pylint-results.json"
            }
        }
    }
}
