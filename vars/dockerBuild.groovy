def call(Map config = [:]) {
    def registry = config.registry ?: 'registry.internal.example.com'
    def image_name = config.image_name ?: env.REPO_NAME
    def tag = env.GIT_SHORT_HASH

    stage("Podman Build") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/podman.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('podman') {
                    sh "podman build -t ${registry}/${image_name}:${tag} ."
                    sh "podman push ${registry}/${image_name}:${tag}"
                    env.IMAGE_TAG = "${registry}/${image_name}:${tag}"
                }
                cosignSign(config)
            }
        }
    }
}
