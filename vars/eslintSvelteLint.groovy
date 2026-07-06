def call(Map config = [:]) {
    def paths = config.paths ?: 'src/'
    def working_dir = config.working_dir ?: '.'
    def fail_on_error = config.fail_on_error != false

    stage("ESLintSvelte") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/node.yaml')) {
            node(POD_LABEL) {
                unstash "workspace"
                container('node') {
                    sh "npm config set registry https://artifactory.cloud.cms.gov/artifactory/api/npm/npm/"
                    sh "npm install --save-dev eslint svelte eslint-plugin-svelte @eslint/js typescript-eslint @sveltejs/eslint-config globals"
                    def exit_code = sh(
                        script: "cd ${working_dir} && npx eslint ${paths} --format json --output-file stdio | tee eslint-results.json",
                        returnStatus: true
                    )
                    if (exit_code != 0 && fail_on_error) {
                        error "ESLint found violations"
                    }
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "${working_dir}/eslint-results.json"
            }
        }
    }
}
