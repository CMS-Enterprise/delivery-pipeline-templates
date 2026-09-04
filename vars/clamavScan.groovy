def call(Map config = [:]) {
    def scan_path = config.scan_path ?: '.'
    def filesize_limit = config.filesize_limit ?: '500M'
    def scansize_limit = config.scansize_limit ?: '1G'
    def output_name = config.output_name ?: 'virus-report'
    def stagename = config.stage ?: 'Security Scan: Malware'
    def image = config.image
    def report = "${output_name}.clamav.txt"
    // Scanning an image means flattening it to a directory tree first, which
    // needs the podman sidecar and replaces the source unstash.
    def pod_default = image ? 'resources/pods/clamav-podman.yaml' : 'resources/pods/clamav.yaml'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted(pod_default)) {
            node(POD_LABEL) {
                if (image) {
                    def rootfs = 'clamav-rootfs'
                    container('podman') {
                        sh """
                            podman pull ${image}
                            cid=\$(podman create ${image})
                            mkdir -p ${rootfs}
                            podman export "\$cid" | tar -x -C ${rootfs}
                            podman rm --force "\$cid"
                        """
                    }
                    scan_path = rootfs
                } else {
                    unstash config.unstash ?: 'workspace'
                }
                container('clamav') {
                    sh "freshclam"
                    def exit_code = sh (
                        script: """
                        clamscan --infected --recursive \
                            --max-filesize=${filesize_limit} \
                            --max-scansize=${scansize_limit} \
                            --max-files=0 \
                            --max-scantime=0 \
                            --log=${report} \
                            --stdout \
                            ${scan_path}
                        """,
                        returnStatus: true
                    )
                    if (exit_code != 0) {
                        sh "cat ${report}"
                        archiveArtifacts allowEmptyArchive: true, artifacts: report
                        error "ClamAV found infected files in ${image ?: scan_path}"
                    }
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: report
            }
        }
    }
}
