def call(Map config = [:], String environment) {
    def ansible_image = config.ansible_image ?: 'artifactory.cloud.cms.gov/docker/ansible/ansible-runner@sha256:a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2'
    def playbook = config.playbook ?: "deploy-${environment}.yml"
    def inventory = config.inventory ?: "inventories/${environment}"
    def extra_vars = config.extra_vars ?: ''

    stage("Ansible Deploy (${environment})") {
        podTemplate(containers: [
            containerTemplate(name: 'ansible', image: ansible_image, command: 'cat', ttyEnabled: true)
        ]) {
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
