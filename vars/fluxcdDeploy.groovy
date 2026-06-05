def call(Map config = [:], String environment) {
    def kustomization = config.kustomization ?: "${env.REPO_NAME}-${environment}"
    def namespace = config.namespace ?: 'flux-system'
    def timeout = config.timeout ?: '5m'

    stage("FluxCD Deploy (${environment})") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/flux.yaml')) {
            node(POD_LABEL) {
                container('flux') {
                    def cluster = config.clusters?."${environment}" ?: 'default-cluster'
                    withCredentials([file(credentialsId: "kubeconfig-${cluster}", variable: 'KUBECONFIG')]) {
                        sh """
                            flux reconcile kustomization ${kustomization} \
                                --namespace ${namespace} \
                                --with-source \
                                --timeout ${timeout}
                        """
                        sh """
                            flux get kustomization ${kustomization} \
                                --namespace ${namespace}
                        """
                    }
                }
            }
        }
    }
}
