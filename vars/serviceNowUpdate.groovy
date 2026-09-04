import groovy.json.JsonOutput

def call(Map config = [:]) {
    def site = (config.site ?: 'https://cms.service-now.com').replaceAll('/+$', '')
    def table = config.table ?: 'incident'
    def sys_id = config.sys_id ?: ''
    def number = config.number ?: ''
    def token_credential = config.token_credential ?: 'servicenow-api-token'
    def fields = (config.fields ?: [:]) as Map

    if (!(table ==~ /[a-z0-9_]+/)) {
        error("Invalid ServiceNow table: ${table}")
    }
    if (sys_id && !(sys_id ==~ /[0-9a-f]{32}/)) {
        error("Invalid ServiceNow sys_id: ${sys_id}")
    }
    if (number && !(number ==~ /[A-Za-z0-9_-]+/)) {
        error("Invalid ServiceNow number: ${number}")
    }

    def payload = [:]
    if (config.short_description) { payload.short_description = config.short_description }
    if (config.description) { payload.description = config.description }
    if (config.comment) { payload.comments = config.comment }
    if (config.work_notes) { payload.work_notes = config.work_notes }
    if (config.state) { payload.state = config.state as String }
    payload.putAll(fields)

    if (!sys_id && !number && !payload.short_description) {
        error("serviceNowUpdate: creating a ${table} record requires short_description")
    }
    if ((sys_id || number) && !payload) {
        error("serviceNowUpdate: nothing to update on ${sys_id ?: number}")
    }

    def result = [:]

    stage("ServiceNow Update") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/curl.yaml')) {
            node(POD_LABEL) {
                container('curl') {
                    withCredentials([string(credentialsId: token_credential, variable: 'SN_TOKEN')]) {
                        writeFile file: 'servicenow-payload.json', text: JsonOutput.toJson(payload)

                        if (!sys_id && number) {
                            def lookup = sh(
                                script: """
                                    curl -sS --fail-with-body \
                                        -H "Authorization: Bearer \$SN_TOKEN" \
                                        -H "Accept: application/json" \
                                        --get \
                                        --data-urlencode 'sysparm_query=number=${number}' \
                                        --data-urlencode 'sysparm_fields=sys_id' \
                                        --data-urlencode 'sysparm_limit=1' \
                                        "${site}/api/now/table/${table}"
                                """,
                                returnStdout: true
                            ).trim()
                            sys_id = readJSON(text: lookup).result?.getAt(0)?.sys_id
                            if (!sys_id) {
                                error("ServiceNow ${table} record not found: ${number}")
                            }
                        }

                        def create = !sys_id
                        def response = sh(
                            script: """
                                curl -sS --fail-with-body \
                                    -X ${create ? 'POST' : 'PATCH'} \
                                    -H "Authorization: Bearer \$SN_TOKEN" \
                                    -H "Content-Type: application/json" \
                                    -H "Accept: application/json" \
                                    -d @servicenow-payload.json \
                                    "${site}/api/now/table/${table}${create ? '' : '/' + sys_id}"
                            """,
                            returnStdout: true
                        ).trim()

                        def record = readJSON(text: response).result
                        result = [sys_id: record.sys_id, number: record.number, table: table]
                        echo "ServiceNow ${table} ${create ? 'created' : 'updated'}: ${result.number} (${result.sys_id})"
                    }
                }
            }
        }
    }

    result
}
