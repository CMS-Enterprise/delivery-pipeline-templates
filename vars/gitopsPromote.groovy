def call(Map config = [:], String environment) {
    def kustomize_image = config.kustomize_image ?: 'artifactory.cloud.cms.gov/docker/k8s-sigs/kustomize@sha256:d4a3b5c6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4'
    def git_image = config.git_image ?: 'artifactory.cloud.cms.gov/docker/alpine/git@sha256:f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2'
    def manifest_repo = config.manifest_repo ?: error("manifest_repo is required")
    def manifest_branch = config.manifest_branch ?: 'main'
    def image_tag = env.IMAGE_TAG ?: env.GIT_SHORT_HASH
    def environment_path = config.environment_path ?: environment
    def target_service = config.target_service ?: env.REPO_NAME
    def image_overrides = config.image_overrides ?: [:]

    stage("GitOps Promote (${environment})") {
        podTemplate(containers: [
            containerTemplate(name: 'kustomize', image: kustomize_image, command: 'cat', ttyEnabled: true),
            containerTemplate(name: 'git', image: git_image, command: 'cat', ttyEnabled: true)
        ]) {
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
