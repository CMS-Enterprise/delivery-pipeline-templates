def call(Map config = [:]) {
    def clamav_image = config.clamav_image ?: 'artifactory.cloud.cms.gov/docker/clamav/clamav@sha256:c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2'
    def scan_path = config.scan_path ?: '.'
    def filesize_limit = config.filesize_limit ?: '500M'
    def scansize_limit = config.scansize_limit ?: '1G'

    stage("ClamAV Antivirus Scan") {
        podTemplate(containers: [
            containerTemplate(name: 'clamav', image: clamav_image, command: 'cat', ttyEnabled: true,
                resourceLimitMemory: '5Gi', resourceRequestMemory: '3Gi')
        ]) {
            node(POD_LABEL) {
                unstash "workspace"
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
