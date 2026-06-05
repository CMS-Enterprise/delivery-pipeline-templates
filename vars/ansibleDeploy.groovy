def call(Map config = [:], String environment) {
    def playbook = config.playbook ?: "deploy-${environment}.yml"
    def inventory = config.inventory ?: "inventories/${environment}"
    def extra_vars = config.extra_vars ?: ''

    stage("Ansible Deploy (${environment})") {
        podTemplate(yaml: config.pod_yaml ?: readTrusted('resources/pods/ansible.yaml')) {
            node(POD_LABEL) {
                checkout scm
                container('ansible') {
                    def credentialId = config.ssh_credential ?: "ansible-ssh-key-${environment}"
                    withCredentials([sshUserPrivateKey(credentialsId: credentialId, keyFileVariable: 'SSH_KEY')]) {
                        sh """
                            ansible-playbook ${playbook} \
                                -i ${inventory} \
                                --private-key=\$SSH_KEY \
                                ${extra_vars ? "-e '${extra_vars}'" : ''}
                        """
                    }
                }
            }
        }
    }
}
