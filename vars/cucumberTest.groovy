def call(Map config = [:]) {
    def task = config.test_task ?: 'cucumber'
    def tags = config.tags ?: ''

    stage("Cucumber Test") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/gradle.yaml')) {
            node(POD_LABEL) {
                unstash "workspace"
                container('gradle') {
                    sh "./gradlew ${task} --no-daemon ${tags ? "-Dcucumber.filter.tags='${tags}'" : ''}"
                }
                cucumber fileIncludePattern: '**/cucumber-reports/*.json', sortingMethod: 'ALPHABETICAL'
            }
        }
    }
}
