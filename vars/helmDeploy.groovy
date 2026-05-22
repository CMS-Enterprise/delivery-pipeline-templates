def call(Map config = [:], String environment) {
    def helm_image = config.helm_image ?: 'artifactory.cloud.cms.gov/docker/alpine/helm@sha256:d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4'
    def release_name = config.release_name ?: "${env.REPO_NAME}-${environment}"
    def chart_path = config.chart_path ?: 'helm/'
    def namespace = config.namespaces?."${environment}" ?: "${env.REPO_NAME}-${environment}"
    def values_file = config.values_file ?: "helm/values-${environment}.yaml"
    def timeout = config.timeout ?: '5m'

    stage("Helm Deploy (${environment})") {
        podTemplate(containers: [
            containerTemplate(name: 'helm', image: helm_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                checkout scm
                container('helm') {
                    def cluster = config.clusters?."${environment}" ?: 'default-cluster'
                    withCredentials([file(credentialsId: "kubeconfig-${cluster}", variable: 'KUBECONFIG')]) {
                        sh """
                            helm upgrade --install ${release_name} ${chart_path} \
                                --namespace ${namespace} \
                                --create-namespace \
                                -f ${values_file} \
                                --set image.tag=${env.GIT_SHORT_HASH} \
                                --timeout ${timeout} \
                                --wait
                        """
                    }
                }
            }
        }
    }
}
