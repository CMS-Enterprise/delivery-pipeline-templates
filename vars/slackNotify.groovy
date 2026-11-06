import groovy.json.JsonOutput

def call(Map config = [:], String message) {
    def channel = config.channel ?: '#ci-notifications'
    def username = config.username ?: 'jenkins'
    def icon_emoji = config.icon_emoji ?: ':jenkins:'
    def webhook_credential = config.webhook_credential ?: 'slack-webhook-url'
    def color = config.color ?: (currentBuild.currentResult == "SUCCESS" ? "good" : "danger")
    def text = "${env.JOB_NAME} - ${message}\n${env.BUILD_URL}"
    def stagename = config.stage ?: 'Slack Notify'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/curl.yaml')) {
            node(POD_LABEL) {
                container('curl') {
                    // Written to a file so quotes in the message cannot break
                    // out of the shell command.
                    writeFile file: 'slack-payload.json', text: JsonOutput.toJson([
                        channel    : channel,
                        username   : username,
                        icon_emoji : icon_emoji,
                        attachments: [[
                            color: color,
                            text : text
                        ]]
                    ])
                    withCredentials([string(credentialsId: webhook_credential, variable: 'SLACK_WEBHOOK_URL')]) {
                        sh 'curl -sf -X POST -H "Content-Type: application/json" --data @slack-payload.json "$SLACK_WEBHOOK_URL"'
                    }
                }
            }
        }
    }
}
