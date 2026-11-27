// Runs body with DOCKER_CONFIG pointing at a registry config built from a Jenkins
// usernamePassword credential, for tools such as cosign that read and push to the
// registry themselves. Call inside a node; the config is removed afterwards.
def call(Map config = [:], Closure body) {
    def registry = config.registry ?: 'artifactory.cloud.cms.gov'
    def credential = config.credential ?: 'jfrog-credentials'
    if (!(registry ==~ /[A-Za-z0-9.-]+(:[0-9]+)?/)) {
        error("registryAuth: invalid registry '${registry}'")
    }

    withCredentials([usernamePassword(credentialsId: credential,
            usernameVariable: 'REG_USER', passwordVariable: 'REG_PASS')]) {
        withEnv(["DOCKER_CONFIG=${env.WORKSPACE}/.registry-auth", "REGISTRY=${registry}"]) {
            // Single-quoted: the credential is expanded by the shell only.
            sh '''
                set +x
                umask 077
                mkdir -p "$DOCKER_CONFIG"
                auth="$(printf '%s:%s' "$REG_USER" "$REG_PASS" | base64 | tr -d '\\n')"
                printf '{"auths":{"%s":{"auth":"%s"}}}' "$REGISTRY" "$auth" > "$DOCKER_CONFIG/config.json"
            '''
            try {
                body()
            } finally {
                sh 'rm -rf "$DOCKER_CONFIG"'
            }
        }
    }
}
