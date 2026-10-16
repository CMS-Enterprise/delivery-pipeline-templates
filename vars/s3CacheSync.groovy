def restore(Map config) {
    def bucket = config.bucket ?: error('s3CacheSync.restore requires bucket')
    def prefix = config.prefix ?: error('s3CacheSync.restore requires prefix')
    def local_dir = config.local_dir ?: error('s3CacheSync.restore requires local_dir')

    container('aws-cli') {
        sh "aws s3 sync s3://${bucket}/${prefix} ${local_dir} --quiet || true"
    }
}

def save(Map config) {
    def bucket = config.bucket ?: error('s3CacheSync.save requires bucket')
    def prefix = config.prefix ?: error('s3CacheSync.save requires prefix')
    def local_dir = config.local_dir ?: error('s3CacheSync.save requires local_dir')

    container('aws-cli') {
        sh "aws s3 sync ${local_dir} s3://${bucket}/${prefix} --quiet"
    }
}
