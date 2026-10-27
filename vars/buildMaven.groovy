def call(Map config = [:]) {
    def stagename = config.stage ?: 'Build: Maven'
    def working_dir = config.working_dir ?: '.'
    def mystash = config.stash ?: 'workspace'
    def myunstash = config.unstash ?: 'workspace'
    def settings_file = config.settings_file ?: 'settings.xml'
    def m2_repo = config.m2_repo ?: '/home/jenkins/agent/.m2/repository'
    def s3_cache_bucket = config.s3_cache_bucket ?: null
    def s3_cache_prefix = config.s3_cache_prefix ?: 'maven-cache'

    def cache_flags = ''
    if (s3_cache_bucket) {
        cache_flags = "-Dmaven.build.cache.remote.url=s3://${s3_cache_bucket}/${s3_cache_prefix}" +
                      " -Dmaven.build.cache.remote.enabled=true" +
                      " -Dmaven.build.cache.remote.save.enabled=true"
    }

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/maven-openjdk25.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                container('maven') {
                    sh "cd ${working_dir} && mvn --no-transfer-progress package -DskipTests -s ${settings_file} -Dmaven.repo.local=${m2_repo} ${cache_flags}"
                }
                stash name: "${mystash}", includes: "${working_dir}/**"
            }
        }
    }
}
