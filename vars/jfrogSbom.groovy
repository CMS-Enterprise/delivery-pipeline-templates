def call(Map config = [:]) {
    def server_id = config.server_id ?: 'jfrog-artifactory'
    def jfrog_url = config.url ?: 'https://artifactory.cloud.cms.gov/artifactory'
    def output_name = config.output_name ?: 'jfrog-sbom'
    def build_name = config.build_name ?: env.JFROG_BUILD_NAME ?: env.JOB_NAME
    def build_number = config.build_number ?: env.JFROG_BUILD_NUMBER ?: env.BUILD_NUMBER
    def stagename = config.stage ?: 'JFrog SBOM Export'
    def project_flag = config.project ? "--project=${config.project}" : ''

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/jfrog-cli.yaml')) {
            node(POD_LABEL) {
                container('jfrog-cli') {
                    withCredentials([usernamePassword(credentialsId: config.credential ?: 'jfrog-credentials', usernameVariable: 'JFROG_USER', passwordVariable: 'JFROG_ACCESS_TOKEN')]) {
                        sh """
                            jf config add ${server_id} \
                                --url=${jfrog_url} \
                                --user=\$JFROG_USER \
                                --access-token=\$JFROG_ACCESS_TOKEN \
                                --interactive=false \
                                --overwrite=true

                            jf rt build-collect-env '${build_name}' ${build_number} ${project_flag}

                            # An empty or non-JSON file must not pass as an SBOM:
                            # cosign attest and the archived artifact depend on it.
                            sbom=${output_name}.cyclonedx.json
                            ok=
                            for attempt in 1 2 3; do
                                rc=0
                                JFROG_CLI_LOG_LEVEL=DEBUG jf sbom-export \
                                    --server-id=${server_id} \
                                    --build-name='${build_name}' \
                                    --build-number=${build_number} \
                                    --format=cyclonedx ${project_flag} \
                                    > "\$sbom" 2> sbom-export.err || rc=\$?
                                if [ "\$rc" -eq 0 ] && [ "\$(tr -d ' \\t\\r\\n' < "\$sbom" | head -c 1)" = '{' ]; then
                                    ok=1
                                    break
                                fi
                                echo "sbom-export attempt \$attempt failed (exit \$rc, \$(wc -c < "\$sbom") bytes)"
                                echo '--- stderr ---'; tail -n 50 sbom-export.err
                                echo '--- stdout ---'; head -c 2000 "\$sbom"; echo
                                [ "\$attempt" -lt 3 ] && sleep 10
                            done
                            if [ -z "\$ok" ]; then
                                rm -f "\$sbom"
                                echo 'sbom-export failed after 3 attempts' >&2
                                exit 1
                            fi
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
