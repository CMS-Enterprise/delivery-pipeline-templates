import groovy.json.JsonOutput

def call(Map config = [:], String message) {
    def channel = config.channel ?: '#ci-notifications'
    def username = config.username ?: 'jenkins'
    def icon_emoji = config.icon_emoji ?: ':jenkins:'
    def webhook_credential = config.webhook_credential ?: 'slack-webhook-url'
    def color = config.color ?: (currentBuild.currentResult == "SUCCESS" ? "good" : "danger")
    def text = "${env.JOB_NAME} - ${message}\n${env.BUILD_URL}"

    stage("Slack Notify") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/curl.yaml')) {
            node(POD_LABEL) {
                container('curl') {
                    def payload = JsonOutput.toJson([
                        channel    : channel,
                        username   : username,
                        icon_emoji : icon_emoji,
                        attachments: [[
                            color: color,
                            text : text
                        ]]
                    ])
                    withCredentials([string(credentialsId: webhook_credential, variable: 'SLACK_WEBHOOK_URL')]) {
                        sh "curl -s -X POST --data-urlencode 'payload=${payload}' \$SLACK_WEBHOOK_URL"
                    }
                }
            }
        }
    }
}
