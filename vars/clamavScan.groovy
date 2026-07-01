def call(Map config = [:]) {
    def scan_path = config.scan_path ?: '.'
    def filesize_limit = config.filesize_limit ?: '500M'
    def scansize_limit = config.scansize_limit ?: '1G'

    stage("ClamAV Antivirus Scan") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/clamav.yaml')) {
            node(POD_LABEL) {
                container('clamav') {
                    sh """
                        freshclam
                        clamscan --infected --recursive \
                            --max-filesize=${filesize_limit} \
                            --max-scansize=${scansize_limit} \
                            --max-files=0 \
                            --max-scantime=0 \
                            --log=virus-report.clamav.txt \
                            --stdout \
                            ${scan_path}
                    """
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "virus-report.clamav.txt"
            }
        }
    }
}
