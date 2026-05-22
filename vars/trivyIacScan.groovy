def call(Map config = [:]) {
    def trivy_image = config.trivy_image ?: 'artifactory.cloud.cms.gov/docker/aquasec/trivy@sha256:b9e3d0c3e0c3f9d6a8e5a7c2f1b0a4d8e6c9f2a5b8d1e4f7a0c3b6d9e2f5a8b1'
    def scan_path = config.iac_scan_path ?: '.'
    def severity = config.iac_severity ?: 'CRITICAL,HIGH,MEDIUM'
    def fail_on_violation = config.iac_fail_on_violation != false
    def skip_dirs = config.iac_skip_dirs ?: '.terraform,node_modules'

    stage("Trivy IaC Scan") {
        podTemplate(containers: [
            containerTemplate(name: 'trivy', image: trivy_image, command: 'cat', ttyEnabled: true)
        ]) {
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
