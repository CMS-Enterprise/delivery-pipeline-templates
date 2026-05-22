def call(Map config = [:], String environment) {
    def kubectl_image = config.kubectl_image ?: 'artifactory.cloud.cms.gov/docker/bitnami/kubectl@sha256:13dc27afebffa1065bf7602d72a2d2e77019647fc11e591cead5e68304c8e914'
    def overlay_path = config.overlay_path ?: "k8s/overlays/${environment}"
    def namespace = config.namespaces?."${environment}" ?: "${env.REPO_NAME}-${environment}"

    stage("Kustomize Deploy (${environment})") {
        podTemplate(containers: [
            containerTemplate(name: 'kubectl', image: kubectl_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                checkout scm
                container('kubectl') {
                    def cluster = config.clusters?."${environment}" ?: 'default-cluster'
                    withCredentials([file(credentialsId: "kubeconfig-${cluster}", variable: 'KUBECONFIG')]) {
                        sh """
                            kubectl apply -k ${overlay_path} -n ${namespace}
                            kubectl rollout status deployment/${env.REPO_NAME} \
                                -n ${namespace} --timeout=300s
                        """
                    }
                }
            }
        }
    }
}
