def call(Map config = [:]) {
    def account_id = config.account_id ?: error('account_id is required for ECR push')
    def region = config.region ?: 'us-east-1'
    def registry = config.registry ?: "${account_id}.dkr.ecr.${region}.amazonaws.com"
    def image_name = config.image_name ?: env.REPO_NAME ?: error('image_name is required (env.REPO_NAME is not set)')
    def tag = config.tag ?: env.GIT_SHORT_HASH ?: error('tag is required (env.GIT_SHORT_HASH is not set)')
    def context_dir = config.context_dir ?: '.'
    def containerfile = config.containerfile ?: "${context_dir}/Dockerfile"
    def myunstash = config.unstash ?: 'workspace'
    def stagename = config.stage ?: 'Podman Build & Push to ECR'
    def image = "${registry}/${image_name}:${tag}"

    stage("${stagename}") {
        if (!env.AWS_ACCESS_KEY_ID) {
            def aws_env = config.aws_environment ?: 'build'
            awsAssumeRole([
                account_ids: [(aws_env): account_id],
                role_name: config.role_name ?: 'deploy-role',
                region: region,
                pod_yaml: config.aws_pod_yaml,
            ], aws_env)
        }
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/podman.yaml')) {
            node(POD_LABEL) {
                unstash "${myunstash}"
                container('podman') {
                    sh """
                        aws ecr get-login-password --region ${region} | \
                            podman login --username AWS --password-stdin ${registry}

                        podman build -f ${containerfile} -t ${image} ${context_dir}

                        podman push ${image}
                    """
                    env.IMAGE_TAG = image
                }
                cosignSign(config + [image: image])
            }
        }
    }
}
