package pods

import rego.v1

deny contains msg if {
    not input.metadata.labels["jenkins-agent"]
    msg := "pod must have label 'jenkins-agent'"
}

deny contains msg if {
    input.metadata.labels["jenkins-agent"] != "true"
    msg := sprintf("label 'jenkins-agent' must be 'true', got '%s'", [input.metadata.labels["jenkins-agent"]])
}
