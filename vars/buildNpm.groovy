def call(Map config = [:]) {
    def stagename = config.stage ?: 'Build: NPM'
    def working_dir = config.working_dir ?: '.'
    def mystash = config.stash ?: 'workspace'
    def myunstash = config.unstash ?: 'workspace'
    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/node.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                container('node') {
                    sh """
                        npm config set registry https://artifactory.cloud.cms.gov/artifactory/api/npm/npm/
                        cd ${working_dir}
                        npm ci
                        npm run build
                    """
                }
                stash name: "${mystash}", includes: "${working_dir}/**"
            }
        }
    }
}
