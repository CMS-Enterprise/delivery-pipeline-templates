def restore(Map config) {
    def bucket = config.bucket ?: error('s3CacheSync.restore requires bucket')
    def prefix = config.prefix ?: error('s3CacheSync.restore requires prefix')
    def local_dir = config.local_dir ?: error('s3CacheSync.restore requires local_dir')

    container('aws-cli') {
        // Passed through the environment and quoted: the prefix defaults to
        // JOB_NAME, which can contain spaces (e.g. "Devops Pipeline/demo").
        withEnv(["CACHE_SRC=s3://${bucket}/${prefix}", "CACHE_DST=${local_dir}"]) {
            sh 'aws s3 sync "$CACHE_SRC" "$CACHE_DST" --only-show-errors || echo "s3CacheSync: restore from $CACHE_SRC failed; building without cache."'
        }
    }
}

def save(Map config) {
    def bucket = config.bucket ?: error('s3CacheSync.save requires bucket')
    def prefix = config.prefix ?: error('s3CacheSync.save requires prefix')
    def local_dir = config.local_dir ?: error('s3CacheSync.save requires local_dir')

    container('aws-cli') {
        withEnv(["CACHE_SRC=${local_dir}", "CACHE_DST=s3://${bucket}/${prefix}"]) {
            // A cache is an optimisation: a missing directory or a failed upload
            // warns rather than failing the build. --only-show-errors (not
            // --quiet) so an AccessDenied is visible in the log.
            sh '''
                if [ ! -d "$CACHE_SRC" ]; then
                    echo "s3CacheSync: $CACHE_SRC does not exist; nothing to save."
                    exit 0
                fi
                aws s3 sync "$CACHE_SRC" "$CACHE_DST" --only-show-errors \\
                    || echo "s3CacheSync: WARNING save to $CACHE_DST failed; cache not updated."
            '''
        }
    }
}
