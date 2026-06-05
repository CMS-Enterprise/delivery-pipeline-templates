def call(Map config = [:], String environment) {
    def release_name = config.release_name ?: "${env.REPO_NAME}-${environment}"
    def chart_path = config.chart_path ?: 'helm/'
    def namespace = config.namespaces?."${environment}" ?: "${env.REPO_NAME}-${environment}"
    def values_file = config.values_file ?: "helm/values-${environment}.yaml"
    def timeout = config.timeout ?: '5m'

    stage("Helm Deploy (${environment})") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/helm.yaml')) {
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
