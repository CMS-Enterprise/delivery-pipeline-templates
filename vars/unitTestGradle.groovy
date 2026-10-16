def call(Map config = [:]) {
    def task = config.test_task ?: 'test'
    def build_cache = config.build_cache ?: false
    def cache_url = config.cache_url ?: null

    def cache_flags = ''
    if (build_cache) {
        cache_flags = '--build-cache'
        if (cache_url) {
            cache_flags += " --init-script ${pwd()}/cache-init.gradle"
        }
    }

    stage("Gradle Test") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/gradle.yaml')) {
            node(POD_LABEL) {
                unstash "workspace"
                if (build_cache && cache_url) {
                    writeFile file: 'cache-init.gradle', text: readTrusted('resources/gradle/cache-init.gradle')
                }
                container('gradle') {
                    if (cache_url) {
                        withEnv(["GRADLE_CACHE_URL=${cache_url}"]) {
                            sh "./gradlew ${task} --no-daemon ${cache_flags}"
                        }
                    } else {
                        sh "./gradlew ${task} --no-daemon ${cache_flags}"
                    }
                }
                junit allowEmptyResults: true, testResults: '**/build/test-results/**/*.xml'
            }
        }
    }
}
