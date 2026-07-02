def call(Map config = [:]) {
    def paths = config.paths ?: 'src/'
    def working_dir = config.working_dir ?: '.'
    def fail_on_error = config.fail_on_error != false

    stage("ESLintVue") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/node.yaml')) {
            node(POD_LABEL) {
                unstash "workspace"
                container('node') {
                    sh "npm install --save-dev eslint next eslint-plugin-vue @eslint/js typescript-eslint globals"
                    def exit_code = sh(
                        script: "cd ${working_dir} && npx -dd eslint ${paths} --format json --output-file eslint-results.json ; cat /home/node/.npm/_logs/*",
                        returnStatus: true
                    )
                    if (exit_code != 0 && fail_on_error) {
                        error "ESLint found violations"
                    }
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "eslint-results.json"
            }
        }
    }
}
