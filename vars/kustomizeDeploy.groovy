def call(Map config = [:], String environment) {
    def overlay_path = config.overlay_path ?: "k8s/overlays/${environment}"
    def namespace = config.namespaces?."${environment}" ?: "${env.REPO_NAME}-${environment}"

    stage("Kustomize Deploy (${environment})") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/kubectl.yaml')) {
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
