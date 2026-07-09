def call(Map config = [:]) {
    stage("go Build") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/go.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('go') {
                    sh """
                        go build -o go-site .
                    """
                }
                stash name: "workspace", includes: "**"
            }
        }
    }
}
