def call(Map config = [:]) {
    def stagename = config.stage ?: 'Build: Go'
    def working_dir = config.working_dir ?: '.'
    def mystash = config.stash ?: 'workspace'
    stage(${stagename}) {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/go.yaml')) {
            node(POD_LABEL) {
                unstash "workspace"
                container('go') {
                    sh """
                        cd ${working_dir}
                        go build -o go-site .
                    """
                }
                stash name: "${mystash}", includes: "${working_dir}/**"
            }
        }
    }
}
