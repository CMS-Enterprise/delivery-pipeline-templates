// Creates or updates a kubernetes.io/dockerconfigjson Secret in the agent
// namespace from a Jenkins usernamePassword credential, and returns its name for
// a pod's imagePullSecrets. Pods that run a built image as a container (see
// appSidecarPod) need one: the kubelet pulls that image itself, outside any
// withCredentials block. Syncing on every call keeps the Secret current when the
// Jenkins credential is rotated, so the Jenkins credential stays the only source.
//
// Requires the agent service account to get, create and patch Secrets in its
// own namespace.
String call(Map config = [:]) {
    def name = config.name ?: 'jfrog-pull'
    def registry = config.registry ?: 'artifactory.cloud.cms.gov'
    def credential = config.credential ?: 'jfrog-credentials'
    def stagename = config.stage ?: 'Image Pull Secret'

    // Both reach the shell and the Secret's metadata, so hold them to the
    // DNS-label and host[:port] shapes Kubernetes and registries accept.
    if (!(name ==~ /[a-z0-9]([-a-z0-9]{0,61}[a-z0-9])?/)) {
        error("imagePullSecret: invalid Secret name '${name}'")
    }
    if (!(registry ==~ /[A-Za-z0-9.-]+(:[0-9]+)?/)) {
        error("imagePullSecret: invalid registry '${registry}'")
    }

    stage("${stagename}") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/kubectl.yaml')) {
            node(POD_LABEL) {
                container('kubectl') {
                    withCredentials([usernamePassword(credentialsId: credential,
                            usernameVariable: 'REG_USER', passwordVariable: 'REG_PASS')]) {
                        withEnv(["SECRET_NAME=${name}", "REGISTRY=${registry}"]) {
                            // Single-quoted: the credential is expanded by the shell,
                            // never interpolated by Groovy. Server-side apply keeps the
                            // payload out of a last-applied-configuration annotation.
                            sh '''
                                set +x
                                umask 077
                                cfg="$(mktemp)"
                                trap 'rm -f "$cfg"' EXIT
                                ns="$(cat /var/run/secrets/kubernetes.io/serviceaccount/namespace)"
                                auth="$(printf '%s:%s' "$REG_USER" "$REG_PASS" | base64 -w0)"
                                printf '{"auths":{"%s":{"auth":"%s"}}}' "$REGISTRY" "$auth" > "$cfg"
                                kubectl create secret generic "$SECRET_NAME" -n "$ns" \
                                    --type=kubernetes.io/dockerconfigjson \
                                    --from-file=.dockerconfigjson="$cfg" \
                                    --dry-run=client -o yaml |
                                    kubectl apply --server-side --force-conflicts \
                                        --field-manager=jenkins-image-pull-secret -f -
                            '''
                        }
                    }
                }
            }
        }
    }
    return name
}
