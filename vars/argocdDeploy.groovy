def call(Map config = [:], String environment) {
    def argocd_image = config.argocd_image ?: 'artifactory.cloud.cms.gov/docker/argoproj/argocd@sha256:b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2'
    def app_name = config.app_name ?: "${env.REPO_NAME}-${environment}"
    def server = config.server ?: 'https://argocd.internal.example.com'
    def sync_timeout = config.sync_timeout ?: 300

    stage("ArgoCD Deploy (${environment})") {
        podTemplate(containers: [
            containerTemplate(name: 'argocd', image: argocd_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                container('argocd') {
                    withCredentials([string(credentialsId: config.token_credential ?: 'argocd-auth-token', variable: 'ARGOCD_AUTH_TOKEN')]) {
                        sh """
                            argocd login ${server} \
                                --auth-token=\$ARGOCD_AUTH_TOKEN \
                                --grpc-web \
                                --insecure

                            argocd app set ${app_name} \
                                --parameter image.tag=${env.GIT_SHORT_HASH}

                            argocd app sync ${app_name} --timeout ${sync_timeout}
                            argocd app wait ${app_name} --timeout ${sync_timeout}
                        """
                    }
                }
            }
        }
    }
}
