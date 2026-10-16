def call(Map config = [:]) {
    def stagename = config.stage ?: 'Test: Go'
    def working_dir = config.working_dir ?: '.'
    def myunstash = config.unstash ?: 'workspace'
    def test_args = config.test_args ?: './...'
    def s3_cache_bucket = config.s3_cache_bucket ?: null
    def s3_cache_prefix = config.s3_cache_prefix ?: "${env.JOB_NAME}/go-cache"
    def cache_dir = config.cache_dir ?: '/tmp/.cache/go-build'
    def default_pod = s3_cache_bucket ? 'resources/pods/go-s3.yaml' : 'resources/pods/go.yaml'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted(default_pod)) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                if (s3_cache_bucket) {
                    s3CacheSync.restore(bucket: s3_cache_bucket, prefix: s3_cache_prefix, local_dir: cache_dir)
                }
                container('go') {
                    sh """
                        cd ${working_dir}
                        go test -v ${test_args}
                    """
                }
                if (s3_cache_bucket) {
                    s3CacheSync.save(bucket: s3_cache_bucket, prefix: s3_cache_prefix, local_dir: cache_dir)
                }
            }
        }
    }
}
