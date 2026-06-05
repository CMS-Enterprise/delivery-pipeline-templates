package pods

import rego.v1

deny contains msg if {
    container := input.spec.containers[i]
    not container.resources
    msg := sprintf("container '%s' must define resources", [container.name])
}

deny contains msg if {
    container := input.spec.containers[i]
    container.resources
    not container.resources.limits
    msg := sprintf("container '%s' must define resource limits", [container.name])
}

deny contains msg if {
    container := input.spec.containers[i]
    container.resources
    not container.resources.requests
    msg := sprintf("container '%s' must define resource requests", [container.name])
}

deny contains msg if {
    container := input.spec.containers[i]
    container.resources.limits
    not container.resources.limits.cpu
    msg := sprintf("container '%s' must define a cpu limit", [container.name])
}

deny contains msg if {
    container := input.spec.containers[i]
    container.resources.limits
    not container.resources.limits.memory
    msg := sprintf("container '%s' must define a memory limit", [container.name])
}

deny contains msg if {
    container := input.spec.containers[i]
    container.resources.limits
    not container.resources.limits["ephemeral-storage"]
    msg := sprintf("container '%s' must define an ephemeral-storage limit", [container.name])
}
