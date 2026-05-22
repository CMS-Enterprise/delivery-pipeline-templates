def call(Map config = [:]) {
    def podman_image = config.podman_image ?: 'artifactory.cloud.cms.gov/docker/podman/stable@sha256:d6f571f9dba42c692281715f4402bdd78884ff16707b08293ebe4cfdea31dbcb'
    def registry = config.registry ?: 'registry.internal.example.com'
    def image_name = config.image_name ?: env.REPO_NAME
    def tag = env.GIT_SHORT_HASH

    stage("Podman Build") {
        podTemplate(containers: [
            containerTemplate(name: 'podman', image: podman_image, command: 'cat', ttyEnabled: true, privileged: true)
        ]) {
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
