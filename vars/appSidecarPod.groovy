// Returns pod_yaml with the application image added as a native sidecar
// (an init container with restartPolicy: Always). The kubelet starts the pod's
// regular containers, jnlp included, only after the sidecar's startupProbe
// passes, so tests never race the app's boot. Requires Kubernetes 1.29+.
// image_pull_secret names a dockerconfigjson Secret in the agent namespace,
// needed when the image lives in a registry the nodes cannot pull anonymously.
String call(Map config = [:], String pod_yaml) {
    def image = config.image ?: error('appSidecarPod: image is required')
    def port = (config.port ?: error('appSidecarPod: port is required')) as Integer
    def name = config.name ?: 'app'
    def run_as_user = (config.run_as_user ?: 1000) as Integer
    def probe_path = config.probe_path ?: '/'
    def startup_seconds = (config.startup_seconds ?: 180) as Integer
    def cpu = config.cpu ?: '1'
    def memory = config.memory ?: '1Gi'
    def image_pull_secret = config.image_pull_secret ?: ''

    def anchor = '\n  containers:\n'
    def at = pod_yaml.indexOf(anchor)
    if (at < 0 || pod_yaml.contains('\n  initContainers:') || pod_yaml.contains('\n  imagePullSecrets:')) {
        error('appSidecarPod: pod_yaml needs a top-level spec.containers list and no initContainers or imagePullSecrets')
    }

    def pull_secrets = image_pull_secret ? """
  imagePullSecrets:
    - name: ${image_pull_secret}""" : ''

    def sidecar = """
  initContainers:
    - name: ${name}
      image: ${image}
      restartPolicy: Always
      ports:
        - containerPort: ${port}
      startupProbe:
        httpGet:
          path: ${probe_path}
          port: ${port}
        periodSeconds: 5
        failureThreshold: ${startup_seconds.intdiv(5)}
      securityContext:
        runAsUser: ${run_as_user}
        runAsNonRoot: true
        allowPrivilegeEscalation: false
        capabilities:
          drop: ["ALL"]
      resources:
        requests:
          cpu: "${cpu}"
          memory: "${memory}"
          ephemeral-storage: "1Gi"
        limits:
          cpu: "${cpu}"
          memory: "${memory}"
          ephemeral-storage: "1Gi\""""

    return pod_yaml.substring(0, at) + pull_secrets + sidecar + pod_yaml.substring(at)
}
