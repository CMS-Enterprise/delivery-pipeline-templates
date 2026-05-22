def call(Map config = [:], String environment) {
    def kubectl_image = config.kubectl_image ?: 'artifactory.cloud.cms.gov/docker/bitnami/kubectl@sha256:13dc27afebffa1065bf7602d72a2d2e77019647fc11e591cead5e68304c8e914'
    def namespace = config.namespaces?."${environment}" ?: "${env.REPO_NAME}-${environment}"
    def cluster = config.clusters?."${environment}" ?: 'default-cluster'
    def timeout = config.deploy_timeout ?: '300s'
    def retries = config.deploy_retries ?: 2

    stage("Rollback (${environment})") {
        podTemplate(containers: [
            containerTemplate(name: 'kubectl', image: kubectl_image, command: 'cat', ttyEnabled: true)
        ]) {
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
