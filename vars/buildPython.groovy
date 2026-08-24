def call(Map config = [:]) {
    def stagename = config.stage ?: 'Build: Python'
    def working_dir = config.working_dir ?: '.'
    def mycommand = config.command ?: 'echo'
    def mystash = config.stash ?: 'workspace'
    def myunstash = config.unstash ?: 'workspace'
    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/python.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                container('python') {
                    sh """
                        pip3.14 config set global.index-url https://artifactory.cloud.cms.gov/artifactory/api/pypi/python/simple
                        cd ${working_dir}
                        pip3.14 install -r requirements.txt
                        ${mycommand}
                    """
                }
                stash name: "${mystash}", includes: "${working_dir}/**"
            }
        }
    }
}
