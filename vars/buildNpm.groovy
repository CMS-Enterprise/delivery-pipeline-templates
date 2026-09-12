def call(Map config = [:]) {
    def stagename = config.stage ?: 'Build: NPM'
    def working_dir = config.working_dir ?: '.'
    def mystash = config.stash ?: 'workspace'
    def myunstash = config.unstash ?: 'workspace'
    def s3_cache_bucket = config.s3_cache_bucket ?: null
    def s3_cache_prefix = config.s3_cache_prefix ?: "${env.JOB_NAME}/next-cache"
    def cache_dir = config.cache_dir ?: "${working_dir}/.next/cache"
    def default_pod = s3_cache_bucket ? 'resources/pods/node-s3.yaml' : 'resources/pods/node.yaml'
    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted(default_pod)) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                if (s3_cache_bucket) {
                    container('aws-cli') {
                        sh """
                            aws s3 sync \
                                s3://${s3_cache_bucket}/${s3_cache_prefix} \
                                ${cache_dir} --quiet || true
                        """
                    }
                }
                container('node') {
                    sh """
                        npm config set registry https://artifactory.cloud.cms.gov/artifactory/api/npm/npm/
                        cd ${working_dir}
                        npm ci
                        npm run build
                    """
                }
                if (s3_cache_bucket) {
                    container('aws-cli') {
                        sh """
                            aws s3 sync \
                                ${cache_dir} \
                                s3://${s3_cache_bucket}/${s3_cache_prefix} --quiet
                        """
                    }
                }
                stash name: "${mystash}", includes: "${working_dir}/**"
            }
        }
    }
}
