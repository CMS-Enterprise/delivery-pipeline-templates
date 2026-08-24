def call(Map config = [:]) {
    def registry = config.registry ?: 'registry.internal.example.com'
    def image_name = config.image_name ?: env.REPO_NAME ?: error('image_name is required (env.REPO_NAME is not set)')
    def tag = config.tag ?: env.GIT_SHORT_HASH ?: error('tag is required (env.GIT_SHORT_HASH is not set)')
    def context_dir = config.context_dir ?: '.'
    def containerfile = config.containerfile ?: "${context_dir}/Dockerfile"
    def myunstash = config.unstash ?: 'workspace'
    def stagename = config.stage ?: 'Podman Build'
    def image = "${registry}/${image_name}:${tag}"

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/podman.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                container('podman') {
                    sh "podman build -f ${containerfile} -t ${image} ${context_dir}"
                    sh "podman push ${image}"
                    env.IMAGE_TAG = image
                }
                cosignSign(config + [image: image])
            }
        }
    }
}
