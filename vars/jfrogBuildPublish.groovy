// Builds, pushes and (unless sign: false) signs one image in a single pod. Unlike jfrogBuild this
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
    def sign = config.containsKey('sign') ? config.sign : true
    def cache_from = config.cache_from ?: null
    def cache_to = config.cache_to ?: null
    // Reproducible builds: the commit time, not "now", becomes the image's
    // created time and clamps file timestamps in the new layers. podman also
    // passes it to the build as the SOURCE_DATE_EPOCH build arg.
    def source_date_epoch = config.source_date_epoch ?: env.GIT_COMMIT_TIMESTAMP
    def repro_flags = source_date_epoch ? " --source-date-epoch=${source_date_epoch} --rewrite-timestamp" : ''
    // Dockerfiles declare ARG OS_PATCH_DATE ahead of their OS upgrade, so a new
    // value misses the --cache-from layer and re-runs it. Defaults to the UTC
    // date: at most a day's staleness, and the rest of the cache still serves.
    def os_patch_date = config.os_patch_date ?: '$(date -u +%F)'

    // Hand the pushed digest from the podman container to jfrog-cli; both share
    // the pod's workspace.
    def digest_file = "image-digest-${image_name}.txt"
    def image_file = "image-file-${image_name}.txt"

    def cache_flags = ''
    if (cache_from) {
        cache_flags += " --cache-from=${cache_from}"
    }
    if (cache_to) {
        cache_flags += " --cache-to=${cache_to}"
    }

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
                            podman login ${registry} \
                                --username=\$JFROG_USER \
                                --password=\$JFROG_ACCESS_TOKEN

                            podman build${repro_flags} \
                                --build-arg OS_PATCH_DATE=${os_patch_date} \
                                -f ${containerfile} \
                                -t ${full_image}${cache_flags} \
                                ${context_dir}

                            podman push --digestfile '${digest_file}' ${full_image}
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

                            # Record the pushed image as build artifacts. Without it the
                            # build info has no modules, and build-promote marks the build
                            # Released while moving nothing to the target repo.
                            printf '%s@%s\\n' '${full_image}' "\$(cat '${digest_file}')" > '${image_file}'
                            jf rt build-docker-create ${repo} \
                                --image-file='${image_file}' \
                                --build-name='${build_name}' \
                                --build-number=${env.BUILD_NUMBER} \
                                --server-id=${server_id} ${project_flag}

                            jf rt build-collect-env '${build_name}' ${env.BUILD_NUMBER} ${project_flag}

                            jf rt build-add-git '${build_name}' ${env.BUILD_NUMBER} ${project_flag}

                            jf rt build-publish '${build_name}' ${env.BUILD_NUMBER} \
                                --server-id=${server_id} ${project_flag}
                        """
                    }
                }
                // sign: false defers signing to the caller, e.g. to sign the promoted
                // image: build-promote moves only the image, not its signature tags.
                if (sign) {
                    cosignSign(config + [image: full_image, stage: "Cosign Sign: ${image_name}"])
                }
            }
        }
    }
    [image: full_image, build_name: build_name, build_number: env.BUILD_NUMBER]
}
