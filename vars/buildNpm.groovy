def call(Map config = [:]) {
    stage("npm Build") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/node.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('node') {
                    sh """
                        npm ci
                        npm run build
                    """
                }
                stash name: "workspace", includes: "**"
            }
        }
    }
}
