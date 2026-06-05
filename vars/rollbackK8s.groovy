def call(Map config = [:], String environment) {
    def namespace = config.namespaces?."${environment}" ?: "${env.REPO_NAME}-${environment}"
    def cluster = config.clusters?."${environment}" ?: 'default-cluster'
    def timeout = config.deploy_timeout ?: '300s'
    def retries = config.deploy_retries ?: 2

    stage("Rollback (${environment})") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/kubectl.yaml')) {
            node(POD_LABEL) {
                container('kubectl') {
                    withCredentials([file(credentialsId: "kubeconfig-${cluster}", variable: 'KUBECONFIG')]) {
                        sh """
                            kubectl rollout undo deployment/${env.REPO_NAME} \
                                -n ${namespace}
                        """
                        retry(retries) {
                            sh """
                                kubectl rollout status deployment/${env.REPO_NAME} \
                                    -n ${namespace} --timeout=${timeout}
                            """
                        }
                    }
                }
            }
        }
    }
}
