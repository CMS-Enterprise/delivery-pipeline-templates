def call(Map config = [:]) {
    def scan_path = config.scan_path ?: '.'
    def filesize_limit = config.filesize_limit ?: '500M'
    def scansize_limit = config.scansize_limit ?: '1G'

    stage("ClamAV Antivirus Scan") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/clamav.yaml')) {
            node(POD_LABEL) {
                unstash "workspace"
                container('clamav') {
                    sh "freshclam"
                    def exit_code = sh (
                        script: """
                        clamscan --infected --recursive \
                            --max-filesize=${filesize_limit} \
                            --max-scansize=${scansize_limit} \
                            --max-files=0 \
                            --max-scantime=0 \
                            --log=virus-report.clamav.txt \
                            --stdout \
                            ${scan_path}
                        """,
                        returnStatus: true
                    )
                    if (exit_code != 0) {
                        sh "cat virus-report.clamav.txt"
                        archiveArtifacts allowEmptyArchive: true, artifacts: "virus-report.clamav.txt"
                        error "ClamAV found infected files"
                    }
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "virus-report.clamav.txt"
            }
        }
    }
}
