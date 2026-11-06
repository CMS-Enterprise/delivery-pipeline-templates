import groovy.json.JsonOutput

def call(Map config = [:]) {
    def site = (config.site ?: 'https://jiraent.cms.gov').replaceAll('/+$', '')
    def issue_key = config.issue_key ?: error('jiraUpdate: issue_key is required')
    def transition = config.transition ?: ''
    def comment = config.comment ?: "Build ${env.BUILD_URL} - ${currentBuild.currentResult}"
    def stagename = config.stage ?: 'Jira Update'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/curl.yaml')) {
            node(POD_LABEL) {
                container('curl') {
                    withCredentials([string(credentialsId: config.token_credential ?: 'jira-api-token', variable: 'JIRA_TOKEN')]) {
                        // Bodies go through files so quotes in the comment cannot
                        // break out of the shell command.
                        if (comment) {
                            writeFile file: 'jira-comment.json', text: JsonOutput.toJson([body: comment])
                            sh """
                                curl -sf -X POST \
                                    -H "Authorization: Bearer \$JIRA_TOKEN" \
                                    -H "Content-Type: application/json" \
                                    --data @jira-comment.json \
                                    "${site}/rest/api/2/issue/${issue_key}/comment"
                            """
                        }
                        if (transition) {
                            writeFile file: 'jira-transition.json', text: JsonOutput.toJson([transition: [id: transition]])
                            sh """
                                curl -sf -X POST \
                                    -H "Authorization: Bearer \$JIRA_TOKEN" \
                                    -H "Content-Type: application/json" \
                                    --data @jira-transition.json \
                                    "${site}/rest/api/2/issue/${issue_key}/transitions"
                            """
                        }
                    }
                }
            }
        }
    }
}
