def call(Map config = [:]) {
    def scan_path = config.iac_scan_path ?: '.'
    def severity = config.iac_severity ?: 'CRITICAL,HIGH,MEDIUM'
    def fail_on_violation = config.iac_fail_on_violation != false
    def skip_dirs = config.iac_skip_dirs ?: '.terraform,node_modules'

    stage("Trivy IaC Scan") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/trivy.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('trivy') {
                    def exit_code = fail_on_violation ? '1' : '0'
                    sh """
                        trivy config ${scan_path} \
                            --severity ${severity} \
                            --exit-code ${exit_code} \
                            --skip-dirs ${skip_dirs} \
                            --format json \
                            --output trivy-iac-results.json
                    """
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "trivy-iac-results.json"
            }
        }
    }
}
