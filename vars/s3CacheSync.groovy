def restore(Map config) {
    def bucket = config.bucket ?: error('s3CacheSync.restore requires bucket')
    def prefix = config.prefix ?: error('s3CacheSync.restore requires prefix')
    def local_dir = config.local_dir ?: error('s3CacheSync.restore requires local_dir')

    container('aws-cli') {
        // Passed through the environment and quoted: the prefix defaults to
        // JOB_NAME, which can contain spaces (e.g. "Devops Pipeline/demo").
        withEnv(["CACHE_SRC=s3://${bucket}/${prefix}", "CACHE_DST=${local_dir}"]) {
            sh 'aws s3 sync "$CACHE_SRC" "$CACHE_DST" --quiet || true'
        }
    }
}

def save(Map config) {
    def bucket = config.bucket ?: error('s3CacheSync.save requires bucket')
    def prefix = config.prefix ?: error('s3CacheSync.save requires prefix')
    def local_dir = config.local_dir ?: error('s3CacheSync.save requires local_dir')

    container('aws-cli') {
        withEnv(["CACHE_SRC=${local_dir}", "CACHE_DST=s3://${bucket}/${prefix}"]) {
            sh 'aws s3 sync "$CACHE_SRC" "$CACHE_DST" --quiet'
        }
    }
}
