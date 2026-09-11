def call(Map config = [:]) {
    def working_dir = config.working_dir ?: '.'
    def stagename = config.stage ?: 'Checkstyle Lint: Maven'
    def myunstash = config.unstash ?: 'workspace'
    def settings_file = config.settings_file ?: 'settings.xml'
    def m2_repo = config.m2_repo ?: '/home/jenkins/agent/.m2/repository'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/maven-openjdk25.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                container('maven') {
                    sh "cd ${working_dir} && mvn checkstyle:check -s ${settings_file} -Dmaven.repo.local=${m2_repo}"
                }
            }
        }
    }
}
