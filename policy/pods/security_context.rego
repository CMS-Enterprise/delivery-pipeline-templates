package pods

import rego.v1

deny contains msg if {
    not input.spec.securityContext.runAsUser
    msg := "pod must set spec.securityContext.runAsUser"
}

deny contains msg if {
    input.spec.restartPolicy != "Never"
    msg := sprintf("pod restartPolicy must be 'Never', got '%s'", [input.spec.restartPolicy])
}

deny contains msg if {
    not input.spec.restartPolicy
    msg := "pod must set spec.restartPolicy"
}
