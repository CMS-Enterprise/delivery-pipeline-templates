def call(Map config = [:]) {
    def task = config.build_task ?: 'assemble'
    def working_dir = config.working_dir ?: '.'
    def stagename = config.stage ?: 'Gradle Build'
    def mystash = config.stash ?: 'workspace'
    def myunstash = config.unstash ?: 'workspace'
    def s3_cache_bucket = config.s3_cache_bucket ?: null
    def cache_flags = s3_cache_bucket ? '--build-cache' : ''

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/gradle.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                container('gradle') {
                    def envVars = []
                    if (s3_cache_bucket) {
                        envVars.add("S3_BUILD_CACHE_BUCKET=${s3_cache_bucket}")
                    }
                    withEnv(envVars) {
                        sh "cd ${working_dir} && gradle ${task} --no-daemon --console=plain ${cache_flags}"
                    }
                }
                stash name: "${mystash}", includes: "${working_dir}/**"
            }
        }
    }
}
