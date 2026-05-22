def call(Map config = [:]) {
    def node_image = config.node_image ?: "artifactory.cloud.cms.gov/docker/node:${config.node_version ?: '20'}"
    def collection = config.collection ?: 'tests/api-contracts.postman_collection.json'
    def environment_file = config.environment_file ?: ''

    stage("Postman/Newman API Test") {
        podTemplate(containers: [
            containerTemplate(name: 'node', image: node_image, command: 'cat', ttyEnabled: true)
        ]) {
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
