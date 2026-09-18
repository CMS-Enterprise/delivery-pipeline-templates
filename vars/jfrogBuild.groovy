def call(Map config = [:]) {
    def server_id = config.server_id ?: 'jfrog-artifactory'
    def registry = config.registry ?: 'docker.artifactory.cloud.cms.gov'
    def repo = config.staging_repo ?: 'docker-staging-local'
    def image_name = config.image_name ?: env.REPO_NAME
    def tag = config.tag ?: env.GIT_SHORT_HASH
    def full_image = "${registry}/${repo}/${image_name}:${tag}"
    def jfrog_url = config.url ?: 'https://artifactory.cloud.cms.gov/artifactory'
    def project_flag = config.project ? "--project=${config.project}" : ''

    stage("Podman Build & Push to JFrog") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/podman-jfrog.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('podman') {
                    withCredentials([usernamePassword(credentialsId: config.credential ?: 'jfrog-credentials', usernameVariable: 'JFROG_USER', passwordVariable: 'JFROG_ACCESS_TOKEN')]) {
                        sh """
                            podman build -t ${full_image} .

                            podman login ${registry} \
                                --username=\$JFROG_USER \
                                --password=\$JFROG_ACCESS_TOKEN

                            podman push ${full_image}
                        """
                    }
                }
                container('jfrog-cli') {
                    withCredentials([usernamePassword(credentialsId: config.credential ?: 'jfrog-credentials', usernameVariable: 'JFROG_USER', passwordVariable: 'JFROG_ACCESS_TOKEN')]) {
                        sh """
                            jf config add ${server_id} \
                                --url=${jfrog_url} \
                                --user=\$JFROG_USER \
                                --access-token=\$JFROG_ACCESS_TOKEN \
                                --interactive=false \
                                --overwrite=true \
                                --ci

                            jf rt build-publish '${env.JOB_NAME}' ${env.BUILD_NUMBER} \
                                --server-id=${server_id} ${project_flag}
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
