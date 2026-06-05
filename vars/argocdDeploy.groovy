def call(Map config = [:], String environment) {
    def app_name = config.app_name ?: "${env.REPO_NAME}-${environment}"
    def server = config.server ?: 'https://argocd.internal.example.com'
    def sync_timeout = config.sync_timeout ?: 300

    stage("ArgoCD Deploy (${environment})") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/argocd.yaml')) {
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
