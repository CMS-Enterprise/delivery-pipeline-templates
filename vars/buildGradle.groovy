def call(Map config = [:]) {
    def task = config.build_task ?: 'assemble'

    stage("Gradle Build") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/gradle.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('gradle') {
                    sh "./gradlew ${task} --no-daemon"
                }
                stash name: "workspace", includes: "**"
            }
        }
    }
}
