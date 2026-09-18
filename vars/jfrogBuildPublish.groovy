// Builds, pushes and signs one image in a single pod. Unlike jfrogBuild this
// takes an explicit image_name and context_dir so a monorepo can fan out one
// invocation per service, and it registers each push under a distinct JFrog
// build name so the per-service Xray scans and promotions stay separate.
def call(Map config = [:]) {
    def server_id = config.server_id ?: 'jfrog-artifactory'
    def registry = config.registry ?: 'artifactory.cloud.cms.gov'
    def repo = config.staging_repo ?: 'docker-staging-local'
    def image_name = config.image_name ?: error('image_name is required')
    def tag = config.tag ?: env.GIT_SHORT_HASH ?: error('tag is required (env.GIT_SHORT_HASH is not set)')
    def context_dir = config.context_dir ?: '.'
    def containerfile = config.containerfile ?: "${context_dir}/Dockerfile"
    def jfrog_url = config.url ?: 'https://artifactory.cloud.cms.gov/'
    def build_name = config.build_name ?: "${env.JOB_NAME}-${image_name}"
    def stagename = config.stage ?: "Build & Publish: ${image_name}"
    def full_image = "${registry}/${repo}/${image_name}:${tag}"
    def project_flag = config.project ? "--project=${config.project}" : ''
    // Reproducible builds need a fixed timestamp rather than "now".
    def source_date_epoch = config.source_date_epoch ?: env.GIT_COMMIT_TIMESTAMP

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/podman-jfrog.yaml')) {
            node(POD_LABEL) {
                unstash 'workspace'
                if (config.unstash && config.unstash != 'workspace') {
                    unstash config.unstash
                }
                container('podman') {
                    withCredentials([usernamePassword(credentialsId: config.credential ?: 'jfrog-credentials', usernameVariable: 'JFROG_USER', passwordVariable: 'JFROG_ACCESS_TOKEN')]) {
                        sh """
                            podman build \
                                ${source_date_epoch ? "--timestamp ${source_date_epoch}" : ''} \
                                -f ${containerfile} \
                                -t ${full_image} \
                                ${context_dir}

                            podman login ${registry} \
                                --username=\$JFROG_USER \
                                --password=\$JFROG_ACCESS_TOKEN

                            podman push ${full_image}
                        """
                    }
                }
                container('jfrog-cli') {
                    withCredentials([usernamePassword(credentialsId: config.credential ?: 'jfrog-credentials', usernameVariable: 'JFROG_USER', passwordVariable: 'JFROG_ACCESS_TOKEN')]) {
                        sh """
                            jf config add ${server_id} \
                                --url=${jfrog_url} \
                                --user=\$JFROG_USER \
                                --access-token=\$JFROG_ACCESS_TOKEN \
                                --interactive=false \
                                --overwrite=true

                            jf rt build-collect-env '${build_name}' ${env.BUILD_NUMBER} ${project_flag}

                            jf rt build-add-git '${build_name}' ${env.BUILD_NUMBER} ${project_flag}

                            jf rt build-publish '${build_name}' ${env.BUILD_NUMBER} \
                                --server-id=${server_id} ${project_flag}
                        """
                    }
                }
                cosignSign(config + [image: full_image, stage: "Cosign Sign: ${image_name}"])
            }
        }
    }
    [image: full_image, build_name: build_name, build_number: env.BUILD_NUMBER]
}
