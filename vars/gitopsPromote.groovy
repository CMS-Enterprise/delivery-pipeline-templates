def call(Map config = [:], String environment) {
    def git_image = config.git_image ?: 'artifactory.cloud.cms.gov/docker/alpine/git@sha256:f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2'
    def manifest_repo = config.manifest_repo ?: error("manifest_repo is required")
    def manifest_branch = config.manifest_branch ?: 'main'
    def image_tag = env.IMAGE_TAG ?: env.GIT_SHORT_HASH
    def environment_path = config.environment_path ?: environment
    def target_service = config.target_service ?: env.REPO_NAME

    stage("GitOps Promote (${environment})") {
        podTemplate(containers: [
            containerTemplate(name: 'git', image: git_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                container('git') {
                    withCredentials([usernamePassword(credentialsId: config.git_credential ?: 'github-org-token', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
                        sh """
                            apk add --no-cache curl tar
                            curl -sSL https://github.com/kubernetes-sigs/kustomize/releases/download/kustomize/v4.5.7/kustomize_v4.5.7_linux_amd64.tar.gz | tar xz -C /usr/local/bin

                            git clone https://\${GIT_USER}:\${GIT_PASS}@${manifest_repo} manifests
                            cd manifests/${environment_path}

                            kustomize edit set image ${target_service}=${image_tag}

                            git config user.email "jenkins@ci.internal"
                            git config user.name "Jenkins Pipeline"
                            git add .
                            git commit -m "deploy ${target_service} to ${environment}: ${image_tag}" || true
                            git push origin ${manifest_branch}
                        """
                    }
                }
            }
        }
    }
}
