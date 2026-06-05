def call(Map config = [:]) {
    def collection = config.collection ?: 'tests/api-contracts.postman_collection.json'
    def environment_file = config.environment_file ?: ''

    stage("Postman/Newman API Test") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/node.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('node') {
                    sh """
                        npx newman run ${collection} \
                            ${environment_file ? "-e ${environment_file}" : ''} \
                            --reporters cli,junit,json \
                            --reporter-junit-export newman-results.xml \
                            --reporter-json-export newman-results.json
                    """
                }
                junit allowEmptyResults: true, testResults: 'newman-results.xml'
                archiveArtifacts allowEmptyArchive: true, artifacts: "newman-results.json"
            }
        }
    }
}
