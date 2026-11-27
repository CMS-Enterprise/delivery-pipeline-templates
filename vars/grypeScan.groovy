def call(Map config = [:]) {
    def image = config.image ?: env.IMAGE_TAG ?: error("image is required (env.IMAGE_TAG is not set)")
    def fail_on_severity = config.containsKey('fail_on_severity') ? config.fail_on_severity : 'high'
    def only_fixed = config.only_fixed ?: false
    def output_name = config.output_name ?: 'grype'
    def stagename = config.stage ?: 'Grype Container Scan'
    // Time-limited exceptions: [vulnerability:, package:, version:, until: 'YYYY-MM-DD',
    // reason:]. Each rule is matched on the exact package version, so an upgrade
    // ends it, and it stops applying after `until` (UTC) without a code change.
    def ignores = config.ignore ?: []
    def ignoreScript = ignores.collect { rule ->
        ['vulnerability', 'package', 'version', 'until', 'reason'].each { key ->
            if (!rule[key]) {
                error("grypeScan ignore rule is missing '${key}': ${rule}")
            }
        }
        // Values are interpolated into the shell, so allow only identifier characters.
        if (!(rule.vulnerability ==~ /[A-Za-z0-9-]+/) || !(rule.package ==~ /[A-Za-z0-9._-]+/) ||
                !(rule.version ==~ /[A-Za-z0-9._+-]+/) || !(rule.until ==~ /\d{4}-\d{2}-\d{2}/)) {
            error("grypeScan ignore rule has an invalid field: ${rule}")
        }
        def untilNum = rule.until.replace('-', '')
        """
                            if [ "\$(date -u +%Y%m%d)" -le ${untilNum} ]; then
                                echo "grype: IGNORING ${rule.vulnerability} in ${rule.package} ${rule.version} until ${rule.until}"
                                printf '  - vulnerability: %s\\n    package:\\n      name: %s\\n      version: %s\\n' \\
                                    '${rule.vulnerability}' '${rule.package}' '${rule.version}' >> grype-ignore.yaml
                            else
                                echo "grype: WARNING exception for ${rule.vulnerability} in ${rule.package} expired ${rule.until}; not applied"
                            fi"""
    }.join('')

    stage("${stagename}") {
        ignores.each { rule ->
            echo "grype exception: ${rule.vulnerability} in ${rule.package} ${rule.version} until ${rule.until}: ${rule.reason}"
        }
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/grype.yaml')) {
            node(POD_LABEL) {
                container('grype') {
                    def pull_registry = image.split('/')[0]
                    withCredentials([usernamePassword(credentialsId: config.credential ?: 'jfrog-credentials', usernameVariable: 'JFROG_USER', passwordVariable: 'JFROG_ACCESS_TOKEN')]) {
                        sh """
                            export DOCKER_CONFIG="\${WORKSPACE:-/home/jenkins/agent}/.docker"
                            mkdir -p "\$DOCKER_CONFIG"
                            AUTH=\$(printf '%s:%s' "\$JFROG_USER" "\$JFROG_ACCESS_TOKEN" | base64 -w0)
                            printf '{"auths":{"%s":{"auth":"%s"}}}' '${pull_registry}' "\$AUTH" > "\$DOCKER_CONFIG/config.json"

                            echo 'ignore:' > grype-ignore.yaml${ignoreScript}

                            grype registry:${image} \
                                -c grype-ignore.yaml \
                                ${only_fixed ? '--only-fixed' : ''} \
                                -o table

                            grype registry:${image} \
                                -c grype-ignore.yaml \
                                ${fail_on_severity ? "--fail-on ${fail_on_severity}" : ''} \
                                ${only_fixed ? '--only-fixed' : ''} \
                                -o json=${output_name}-results.json
                        """
                    }
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: "${output_name}-results.json"
            }
        }
    }
}
