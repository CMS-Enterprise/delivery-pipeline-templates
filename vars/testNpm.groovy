def call(Map config = [:]) {
    def stagename = config.stage ?: 'Test: NPM'
    def working_dir = config.working_dir ?: '.'
    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/node.yaml')) {
            node(POD_LABEL) {
                unstash "workspace"
                container('node') {
                    sh """
                        npm config set registry https://artifactory.cloud.cms.gov/artifactory/api/npm/npm/
                        cd ${working_dir}
                        npm run check
                        npm test
                    """
                }
                stash name: "workspace", includes: "**"
            }
        }
    }
}
