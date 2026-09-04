def call(Map config = [:]) {
    def test_path = config.test_path ?: 'tests/'
    def source_path = config.source_path ?: 'src/'
    def coverage = config.coverage != false
    def requirements_file = config.requirements_file ?: 'requirements-dev.txt'
    def stagename = config.stage ?: 'Test: Python'
    def working_dir = config.working_dir ?: '.'
    def myunstash = config.unstash ?: 'workspace'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/python.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                container('python') {
                    sh """
                        pip3.14 config set global.index-url https://artifactory.cloud.cms.gov/artifactory/api/pypi/python/simple
                        pip3.14 install pytest pytest-cov
                        cd ${working_dir}
                        [ -f ${requirements_file} ] && pip3.14 install -r ${requirements_file}
                        pytest ${test_path} \
                            ${coverage ? "--cov=${source_path} --cov-report=xml:coverage.xml --cov-report=html:htmlcov" : ''} \
                            --junitxml=pytest-results.xml \
                            -v
                    """
                }
                junit allowEmptyResults: true, testResults: "${working_dir}/pytest-results.xml"
                if (coverage) {
                    // archiveArtifacts, not publishHTML: the HTML Publisher plugin
                    // is not installed on the controller, so publishHTML throws
                    // NoSuchMethodError and fails the stage after the tests pass.
                    archiveArtifacts allowEmptyArchive: true,
                        artifacts: "${working_dir}/coverage.xml,${working_dir}/htmlcov/**"
                }
            }
        }
    }
}
