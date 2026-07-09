def call(Map config = [:]) {
    stage("npm Build") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/node.yaml')) {
            node(POD_LABEL) {
                unstash "workspace"
                container('node') {
                    sh """
                        npm config set registry https://artifactory.cloud.cms.gov/artifactory/api/npm/npm/
                        npm ci
                        npm run build
                    """
                }
                stash name: "workspace", includes: "**"
            }
        }
    }
}
