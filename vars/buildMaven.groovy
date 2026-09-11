def call(Map config = [:]) {
    def stagename = config.stage ?: 'Build: Maven'
    def working_dir = config.working_dir ?: '.'
    def mystash = config.stash ?: 'workspace'
    def myunstash = config.unstash ?: 'workspace'
    def settings_file = config.settings_file ?: 'settings.xml'
    def m2_repo = config.m2_repo ?: '/home/jenkins/agent/.m2/repository'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/maven-openjdk25.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                container('maven') {
                    sh "cd ${working_dir} && mvn package -DskipTests -s ${settings_file} -Dmaven.repo.local=${m2_repo}"
                }
                stash name: "${mystash}", includes: "${working_dir}/**"
            }
        }
    }
}
