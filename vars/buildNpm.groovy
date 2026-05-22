def call(Map config = [:]) {
    def image = config.node_image ?: "artifactory.cloud.cms.gov/docker/node:${config.node_version ?: '20'}"

    stage("npm Build") {
        podTemplate(containers: [
            containerTemplate(name: 'node', image: image, command: 'cat', ttyEnabled: true)
        ]) {
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
