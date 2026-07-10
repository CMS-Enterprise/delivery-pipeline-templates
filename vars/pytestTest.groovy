def call(Map config = [:]) {
    def test_path = config.test_path ?: 'tests/'
    def source_path = config.source_path ?: 'src/'
    def coverage = config.coverage != false
    def requirements_file = config.requirements_file ?: 'requirements-dev.txt'

    stage("Pytest") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/python.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('python') {
                    sh """
                        pip3 config set global.index-url https://artifactory.cloud.cms.gov/artifactory/api/pypi/python/simple
                        pip3 install pytest pytest-cov
                        [ -f ${requirements_file} ] && pip3 install -r ${requirements_file}
                        pytest ${test_path} \
                            ${coverage ? "--cov=${source_path} --cov-report=xml:coverage.xml --cov-report=html:htmlcov" : ''} \
                            --junitxml=pytest-results.xml \
                            -v
                    """
                }
                junit allowEmptyResults: true, testResults: 'pytest-results.xml'
                if (coverage) {
                    publishHTML(target: [
                        reportDir: "htmlcov",
                        reportFiles: "index.html",
                        reportName: "Pytest Coverage Report"
                    ])
                }
            }
        }
    }
}
