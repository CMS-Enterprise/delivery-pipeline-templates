def call(Map config = [:]) {
    def podman_image = config.podman_image ?: 'artifactory.cloud.cms.gov/docker/podman/stable@sha256:d6f571f9dba42c692281715f4402bdd78884ff16707b08293ebe4cfdea31dbcb'
    def jfrog_cli_image = config.jfrog_cli_image ?: 'releases-docker.jfrog.io/jfrog/jfrog-cli-v2@sha256:4a7d5c8e9f2b1d6a3c8e5f7b0d2a4e6c9f1b3d5a7c9e2f4b6d8a0c3e5f7a9b1d'
    def server_id = config.server_id ?: 'jfrog-artifactory'
    def registry = config.registry ?: 'docker.artifactory.cloud.cms.gov'
    def repo = config.staging_repo ?: 'docker-staging-local'
    def image_name = config.image_name ?: env.REPO_NAME
    def tag = config.tag ?: env.GIT_SHORT_HASH
    def full_image = "${registry}/${repo}/${image_name}:${tag}"
    def jfrog_url = config.url ?: 'https://artifactory.cloud.cms.gov/artifactory'

    stage("Podman Build & Push to JFrog") {
        podTemplate(containers: [
            containerTemplate(name: 'podman', image: podman_image, command: 'cat', ttyEnabled: true),
            containerTemplate(name: 'jfrog-cli', image: jfrog_cli_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                checkout scm
                container('podman') {
                    sh "podman build -t ${full_image} ."
                }
                container('jfrog-cli') {
                    withCredentials([usernamePassword(credentialsId: config.credential ?: 'jfrog-credentials', usernameVariable: 'JFROG_USER', passwordVariable: 'JFROG_PASS')]) {
                        sh """
                            jf config add ${server_id} \
                                --url=${jfrog_url} \
                                --user=\$JFROG_USER \
                                --password=\$JFROG_PASS \
                                --interactive=false \
                                --overwrite=true

                            jf podman push ${full_image} ${repo} \
                                --server-id=${server_id} \
                                --build-name=${env.JOB_NAME} \
                                --build-number=${env.BUILD_NUMBER}

                            jf rt build-publish ${env.JOB_NAME} ${env.BUILD_NUMBER} \
                                --server-id=${server_id}
                        """
                    }
                }
                env.IMAGE_TAG = full_image
                env.JFROG_BUILD_NAME = env.JOB_NAME
                env.JFROG_BUILD_NUMBER = env.BUILD_NUMBER
                cosignSign(config)
            }
        }
    }
}
