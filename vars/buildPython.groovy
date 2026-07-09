def call(Map config = [:]) {
    stage("python Build") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/python.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('node') {
                    sh """
                        pip3.14 config set global.index-url https://artifactory.cloud.cms.gov/artifactory/api/pypi/python/simple 
                        pip3.14 install -r requirements.txt
                    """
                }
                stash name: "workspace", includes: "**"
            }
        }
    }
}
