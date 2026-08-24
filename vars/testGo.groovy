def call(Map config = [:]) {
    def stagename = config.stage ?: 'Test: Go'
    def working_dir = config.working_dir ?: '.'
    def myunstash = config.unstash ?: 'workspace'
    def test_args = config.test_args ?: './...'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/go.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                container('go') {
                    sh """
                        cd ${working_dir}
                        go test -v ${test_args}
                    """
                }
            }
        }
    }
}
