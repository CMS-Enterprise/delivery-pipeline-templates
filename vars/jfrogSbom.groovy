// Exports a CycloneDX SBOM for an image Xray has already indexed, through
// Xray's Export Component Details API. The SBOM comes from Xray itself, so
// `jf sbom-enrich` (which adds Xray data to SBOMs from other tools) is not
// needed. The result is archived and stashed for cosignAttest.
//
// Export Component Details supports docker components but not builds, so this
// takes the image reference rather than a build name.
def call(Map config = [:]) {
    def server_id = config.server_id ?: 'jfrog-artifactory'
    def jfrog_url = config.url ?: 'https://artifactory.cloud.cms.gov/'
    def registry = config.registry ?: 'artifactory.cloud.cms.gov'
    def image = config.image ?: env.IMAGE_TAG ?: error('jfrogSbom: image is required')
    def output_name = config.output_name ?: 'jfrog-sbom'
    def stagename = config.stage ?: 'JFrog SBOM Export'

    // registry/repo/path/name:tag -> repo, path/name, tag
    def ref = image.startsWith("${registry}/") ? image.substring(registry.length() + 1) : image
    if (ref.contains('@')) {
        error("jfrogSbom: ${image} is a digest reference; Export Component Details needs a tag")
    }
    def colon = ref.lastIndexOf(':')
    def slash = ref.indexOf('/')
    if (colon < 0 || slash < 0 || colon < slash) {
        error("jfrogSbom: cannot parse ${image} as <registry>/<repo>/<name>:<tag>")
    }
    def repo = ref.substring(0, slash)
    def name = ref.substring(slash + 1, colon)
    def tag = ref.substring(colon + 1)

    def request = groovy.json.JsonOutput.toJson([
        package_type: 'docker',
        component_name: "${name}:${tag}",
        path: "${repo}/${name}/${tag}/manifest.json",
        cyclonedx: true,
        cyclonedx_format: 'json',
    ])

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/jfrog-cli.yaml')) {
            node(POD_LABEL) {
                container('jfrog-cli') {
                    writeFile file: 'sbom-request.json', text: request
                    withCredentials([usernamePassword(credentialsId: config.credential ?: 'jfrog-credentials', usernameVariable: 'JFROG_USER', passwordVariable: 'JFROG_ACCESS_TOKEN')]) {
                        sh """
                            jf config add ${server_id} \
                                --url=${jfrog_url} \
                                --user=\$JFROG_USER \
                                --access-token=\$JFROG_ACCESS_TOKEN \
                                --interactive=false \
                                --overwrite=true

                            sbom='${output_name}.cyclonedx.json'
                            rm -f "\$sbom"

                            # Xray may still be indexing a freshly pushed image, so
                            # the export is retried. It answers with a zip.
                            ok=
                            for attempt in 1 2 3 4 5; do
                                rm -rf sbom-export sbom-export.zip
                                code=\$(jf xr curl --server-id=${server_id} -sS \
                                    -XPOST /api/v2/component/exportDetails \
                                    -H 'Content-Type: application/json' \
                                    -d @sbom-request.json \
                                    -o sbom-export.zip -w '%{http_code}') || code=curl-error
                                if [ "\$code" = 200 ] && mkdir sbom-export && unzip -q sbom-export.zip -d sbom-export; then
                                    ok=1
                                    break
                                fi
                                echo "exportDetails attempt \$attempt failed (HTTP \$code)"
                                head -c 2000 sbom-export.zip 2>/dev/null; echo
                                [ "\$attempt" -lt 5 ] && sleep 15
                            done
                            [ -n "\$ok" ] || { echo 'jfrogSbom: Xray SBOM export failed' >&2; exit 1; }

                            exported=
                            for f in \$(find sbom-export -type f -name '*.json' | sort); do
                                if jq -e '.bomFormat == "CycloneDX"' "\$f" >/dev/null 2>&1; then
                                    exported="\$f"
                                    break
                                fi
                            done
                            if [ -z "\$exported" ]; then
                                echo 'jfrogSbom: the export contains no CycloneDX JSON:' >&2
                                find sbom-export -type f >&2
                                exit 1
                            fi
                            mv "\$exported" "\$sbom"
                            echo "SBOM: \$(jq '.components | length' "\$sbom") components"
                        """
                    }
                    archiveArtifacts artifacts: "${output_name}.cyclonedx.json"
                    stash name: "${output_name}", includes: "${output_name}.cyclonedx.json"
                }
            }
        }
    }
    "${output_name}.cyclonedx.json"
}
