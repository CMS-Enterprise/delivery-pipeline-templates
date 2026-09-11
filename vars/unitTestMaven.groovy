def call(Map config = [:]) {
    def stagename = config.stage ?: 'Test: Maven'
    def working_dir = config.working_dir ?: '.'
    def myunstash = config.unstash ?: 'workspace'
    def settings_file = config.settings_file ?: 'settings.xml'
    def m2_repo = config.m2_repo ?: '/home/jenkins/agent/.m2/repository'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/maven-openjdk25.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                container('maven') {
                    sh "cd ${working_dir} && mvn test -s ${settings_file} -Dmaven.repo.local=${m2_repo}"
                }
                junit allowEmptyResults: true, testResults: "${working_dir}/**/target/surefire-reports/*.xml"
            }
        }
    }
}
