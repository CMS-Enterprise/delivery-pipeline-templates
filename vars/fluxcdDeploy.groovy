def call(Map config = [:], String environment) {
    def flux_image = config.flux_image ?: 'artifactory.cloud.cms.gov/docker/fluxcd/flux-cli@sha256:e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2'
    def kustomization = config.kustomization ?: "${env.REPO_NAME}-${environment}"
    def namespace = config.namespace ?: 'flux-system'
    def timeout = config.timeout ?: '5m'

    stage("FluxCD Deploy (${environment})") {
        podTemplate(containers: [
            containerTemplate(name: 'flux', image: flux_image, command: 'cat', ttyEnabled: true)
        ]) {
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
