# Jenkins Secret Provisioner

This is a utility pipeline template to create a Pipeline that can provision a Kubernetes Secret from a Jenkins Credential so that it can be used by other Pipelines. This is useful when you have a secret that is stored in Jenkins that needs to be used by a Kubernetes Pod used to run a pipeline, such as a Docker Registry password.

## Parameters

| Parameter Name        | Description                                                                          |
|-----------------------|--------------------------------------------------------------------------------------|
| secret_name           | The name of the Kubernetes Secret to create.                                         |
| secret_type           | The type of the Kubernetes Secret to create (either generic or docker-registry).     |
| registry_path         | When creating a docker-registry secret, the path to the registry (e.g. `docker.io`). |
| generic_key           | When creating a generic secret, the key to associate the secret value with.          |
| jenkins_credential_id | The Id of the Credential in Jenkins to use as the secret value.                      |


## Creating Docker Registry Secrets

When creating a Docker Registry Secret, it is assumed that the credential provided is a Username and Password type credential. Additionally, the registry_path parameter should be set to the path to the registry (e.g. `docker.io`).

## Creating a Generic Secret

When creating a Generic Secret, the generic_key parameter should be set to the key that the secret value should be associated with in the Secret. Secrets with multiple key-value pairs are not supported.

## Jenkins Configuration

In order for a Jenkins pipeline to be able to managed Kubernetes Secrets, the Kubernetes ServiceAccount that the Jenkins pipeline runs as needs to be granted the requisite permissions. To request that the ServiceAccount be granted the necessary permissions, file a CMS Cloud Support Access Request and include the name of your Jenkins Controller and the permissions your ServiceAccount will need (i.e. Create and Delete Secrets).
