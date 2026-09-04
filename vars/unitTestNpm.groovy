def call(Map config = [:]) {
    stage("npm Test") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/node.yaml')) {
            node(POD_LABEL) {
                unstash "workspace"
                container('node') {
                    sh "npm config set registry https://artifactory.cloud.cms.gov/artifactory/api/npm/npm/"
                    sh "npm test -- --coverage"
                }
                // archiveArtifacts, not publishHTML: the HTML Publisher plugin is
                // not installed on the controller, so publishHTML throws
                // NoSuchMethodError and fails the stage after the tests pass.
                archiveArtifacts allowEmptyArchive: true, artifacts: 'coverage/**'
            }
        }
    }
}
