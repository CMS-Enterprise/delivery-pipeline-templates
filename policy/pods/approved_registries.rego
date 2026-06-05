package pods

import rego.v1

approved_prefixes := [
    "artifactory.cloud.cms.gov/",
    "releases-docker.jfrog.io/",
]

deny contains msg if {
    container := input.spec.containers[i]
    not image_from_approved_registry(container.image)
    msg := sprintf("container '%s' uses unapproved image registry: '%s'", [container.name, container.image])
}

image_from_approved_registry(image) if {
    some prefix in approved_prefixes
    startswith(image, prefix)
}
