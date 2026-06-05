def call(Map config = [:]) {
    def server_id = config.server_id ?: 'jfrog-artifactory'
    def registry = config.registry ?: 'docker.artifactory.cloud.cms.gov'
    def repo = config.staging_repo ?: 'docker-staging-local'
    def image_name = config.image_name ?: env.REPO_NAME
    def tag = config.tag ?: env.GIT_SHORT_HASH
    def full_image = "${registry}/${repo}/${image_name}:${tag}"
    def jfrog_url = config.url ?: 'https://artifactory.cloud.cms.gov/artifactory'

    stage("Podman Build & Push to JFrog") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/podman-jfrog.yaml')) {
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
