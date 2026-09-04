def call(Map config = [:]) {
    def image = config.image ?: env.IMAGE_TAG ?: error('image is required (env.IMAGE_TAG is not set)')
    def profile = config.profile ?: 'xccdf_org.ssgproject.content_profile_stig'
    // Shipped inside the openscap image so no SCAP content is fetched at scan time.
    def datastream = config.datastream ?: '/usr/share/xml/scap/ssg/content/ssg-rhel9-ds.xml'
    def output_name = config.output_name ?: 'openscap'
    def fail_on_violation = config.fail_on_violation != null ? config.fail_on_violation : true
    def stagename = config.stage ?: 'OpenSCAP Policy Scan'
    def rootfs = 'openscap-rootfs'

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/openscap.yaml')) {
            node(POD_LABEL) {
                if (!config.skip_verify) {
                    cosignVerify(config + [image: image])
                }
                container('podman') {
                    // oscap-chroot needs a plain directory tree, and exporting a
                    // flattened container avoids mounting the image as root.
                    sh """
                        podman pull ${image}
                        cid=\$(podman create ${image})
                        mkdir -p ${rootfs}
                        podman export "\$cid" | tar -x -C ${rootfs}
                        podman rm --force "\$cid"
                    """
                }
                container('openscap') {
                    def exit_code = sh(
                        script: """
                            oscap-chroot ${rootfs} xccdf eval \
                                --profile ${profile} \
                                --results ${output_name}-results.xml \
                                --report ${output_name}-report.html \
                                ${datastream}
                        """,
                        returnStatus: true
                    )
                    // Archived only, not rendered inline: the HTML Publisher plugin
                    // is not installed on the controller, so publishHTML throws
                    // NoSuchMethodError and fails the scan after oscap runs.
                    archiveArtifacts allowEmptyArchive: true,
                        artifacts: "${output_name}-results.xml,${output_name}-report.html"
                    // oscap exits 0 when every rule passes, 2 when at least one
                    // rule fails, and 1 for tool errors. Only 2 is a policy result.
                    if (exit_code == 2) {
                        if (fail_on_violation) {
                            error "OpenSCAP found policy violations in ${image}"
                        }
                        unstable "OpenSCAP found policy violations in ${image}"
                    } else if (exit_code != 0) {
                        error "OpenSCAP failed to evaluate ${image} (exit ${exit_code})"
                    }
                }
            }
        }
    }
}
