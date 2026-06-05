def call(Map config = [:], String environment) {
    def manifest_repo = config.manifest_repo ?: error("manifest_repo is required")
    def manifest_branch = config.manifest_branch ?: 'main'
    def image_tag = env.IMAGE_TAG ?: env.GIT_SHORT_HASH
    def environment_path = config.environment_path ?: environment
    def target_service = config.target_service ?: env.REPO_NAME
    def image_overrides = config.image_overrides ?: [:]

    stage("GitOps Promote (${environment})") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/kustomize.yaml')) {
            node(POD_LABEL) {
                container('git') {
                    withCredentials([usernamePassword(credentialsId: config.git_credential ?: 'github-org-token', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
                        sh """
                            git clone https://\${GIT_USER}:\${GIT_PASS}@${manifest_repo} manifests
                        """
                    }
                }
                container('kustomize') {
                    dir("manifests/${environment_path}") {
                        if (image_overrides) {
                            image_overrides.each { name, image ->
                                sh "kustomize edit set image ${name}=${image}"
                            }
                        } else {
                            sh "kustomize edit set image ${target_service}=${image_tag}"
                        }
                    }
                }
                container('git') {
                    withCredentials([usernamePassword(credentialsId: config.git_credential ?: 'github-org-token', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
                        dir('manifests') {
                            sh """
                                git config user.email "jenkins@ci.internal"
                                git config user.name "Jenkins Pipeline"
                                git add .
                                git commit -m "deploy to ${environment}: build ${env.BUILD_NUMBER}"
                                git push origin ${manifest_branch}
                            """
                        }
                    }
                }
            }
        }
    }
}
