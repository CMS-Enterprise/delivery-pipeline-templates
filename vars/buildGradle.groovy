def call(Map config = [:]) {
    def task = config.build_task ?: 'assemble'
    def working_dir = config.working_dir ?: '.'
    def stagename = config.stage ?: 'Gradle Build'
    def mystash = config.stash ?: 'workspace'
    def myunstash = config.unstash ?: 'workspace'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/gradle.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                container('gradle') {
                    sh "cd ${working_dir} && gradle ${task} --no-daemon --console=plain"
                }
                stash name: "${mystash}", includes: "${working_dir}/**"
            }
        }
    }
}
