def call(Map config = [:]) {
    def curl_image = config.curl_image ?: 'artifactory.cloud.cms.gov/docker/curlimages/curl@sha256:eb411f0a02b75f2c2342dbc2f6579905979dd65f61f1b3047067829bb553d149'
    def site = config.site ?: 'https://jiraent.cms.gov/'
    def issue_key = config.issue_key ?: ''
    def transition = config.transition ?: ''
    def comment = config.comment ?: "Build ${env.BUILD_URL} - ${currentBuild.currentResult}"

    stage("Jira Update") {
        podTemplate(containers: [
            containerTemplate(name: 'curl', image: curl_image, command: 'cat', ttyEnabled: true)
        ]) {
            node(POD_LABEL) {
                container('curl') {
                    withCredentials([string(credentialsId: config.token_credential ?: 'jira-api-token', variable: 'JIRA_TOKEN')]) {
                        if (comment) {
                            sh """
                                curl -s -H "Authorization Bearer: ${JIRA_TOKEN}" \
                                    -X POST \
                                    -H "Content-Type: application/json" \
                                    -d '{"body": "${comment}"}' \
                                    "${site}/rest/api/2/issue/${issue_key}/comment"
                            """
                        }
                        if (transition) {
                            sh """
                                curl -s -H "Authorization Bearer: ${JIRA_TOKEN}" \
                                    -X POST \
                                    -H "Content-Type: application/json" \
                                    -d '{"transition": {"id": "${transition}"}}' \
                                    "${site}/rest/api/2/issue/${issue_key}/transitions"
                            """
                        }
                    }
                }
            }
        }
    }
}
