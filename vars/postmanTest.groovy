// newman is installed by `npm ci` from working_dir's package-lock.json, so the
// download is pinned by hash rather than fetched by npx at run time.
def call(Map config = [:]) {
    def collection = config.collection ?: 'tests/api-contracts.postman_collection.json'
    def environment_file = config.environment_file ?: ''
    def env_vars = config.env_vars ?: [:]
    def working_dir = config.working_dir ?: '.'
    def myunstash = config.unstash ?: 'workspace'
    def output_name = config.output_name ?: 'newman'
    def stagename = config.stage ?: 'Postman/Newman API Test'
    def registry = config.npm_registry ?: 'https://artifactory.cloud.cms.gov/artifactory/api/npm/npm/'
    def env_flags = env_vars.collect { k, v -> "--env-var '${k}=${v}'" }.join(' ')

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/node.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                def status = 0
                container('node') {
                    status = sh(returnStatus: true, script: """
                        cd ${working_dir}
                        export npm_config_cache="\$PWD/.npm"
                        npm ci --registry=${registry}
                        npx newman run ${collection} \
                            ${environment_file ? "-e ${environment_file}" : ''} \
                            ${env_flags} \
                            --reporters cli,junit,json \
                            --reporter-junit-export ${output_name}-results.xml \
                            --reporter-json-export ${output_name}-results.json
                    """)
                }
                junit allowEmptyResults: true, testResults: "${working_dir}/${output_name}-results.xml"
                archiveArtifacts allowEmptyArchive: true, artifacts: "${working_dir}/${output_name}-results.json"
                if (status != 0) {
                    error "Newman run failed (exit ${status})"
                }
            }
        }
    }
}
