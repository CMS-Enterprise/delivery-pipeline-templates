# Deployment Pipeline

The Deployment Pipeline clones a Git repository containing Kubernetes manifests, updates the manifests with a new container image, and pushes the changes back to the repository.

# Parameters

The Deployment Pipeline is a [parameterized pipeline](https://www.jenkins.io/doc/book/pipeline/syntax/#parameters) that is intended to be invoked from another pipeline. Some of the parameters can be given default values when an instance of the template is created.

| Parameter Name           | Pipeline | Template | Description                                                                                                                                               | Default Value       |
|--------------------------|----------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------|
| git_manifest_repository  |          | X        | The URL of the Git repository containing Kubernetes manifests.                                                                                            |                     |
| git_manifest_credentials |          | X        | Jenkins credential ID for accessing the Git repository.                                                                                                   |                     |
| git_manifest_branch      | X        | X        | The branch name to checkout.                                                                                                                              | main                |
| git_author_name          | X        | X        | The name to use as the author of git commits.                                                                                                             | Deployment Pipeline |
| git_author_email         | X        | X        | The email address to use as the author of git commits.                                                                                                    |                     |
| environment_path         | X        | X        | Path to the environment directory in the manifest repository.                                                                                             | dev                 |
| target_service           | X        | X        | The name of the service to update.                                                                                                                        |                     |
| image                    | X        | X        | The fully qualified container image name (including the registry).                                                                                        |                     |
| tag                      | X        |          | The tag of the container image being deployed.                                                                                                            |                     |
| maximum_retries          | X        | X        | The maximum number of retries to attempt when applying changes to the manifest repo. Each retry will be delayed by 1.5 ^ n seconds up to a maximum of 30. | 3                   |

# Usage

For general usage see the [CMS ADO Pipeline Catalog README](../../README.md).

The Deployment Pipeline is intended to be invoked dynamically from another Pipeline using the [Pipeline: Build Step plugin](https://plugins.jenkins.io/pipeline-build-step/). This allows a project-specific Jenkins pipeline that is responsible for building and testing the application, using custom tools and scripts for that purpose, to invoke the standard Deployment Pipeline to subsequently update and push the Kubernetes manifests. There is no specific requirement for how that project-specific pipeline is structured, as long as it can invoke the Deployment Pipeline with the required parameters. As an example, a Jenkinsfile that uses the Build Step could be checked into a repository and a [Multi-Branch Pipeline](https://www.jenkins.io/doc/book/pipeline/multibranch/) could be configured for that repository.

```groovy
pipeline {
  stages {
    // Stages and steps that are specific to the project should be specified
    // before the Deployment stage

    stage('Deployment') {
      steps {
        // The job name here corresponds to the name assigned to the instance of the Deployment
        // Pipeline created in Jenkins from the Deployment Pipeline Template.
        build(job: 'K8s Deployment', wait: true, propagate: true, parameters: [
          string(name: 'git_manifest_branch', value: "main"),
          string(name: 'git_author_email', value: "your-email@example.com"),
          string(name: 'environment_path', value: 'dev'),
          string(name: 'target_service', value: 'my-service'),
          string(name: 'image', value: "artifactory.cloud.cms.gov/your-account/your-app"),
          string(name: 'tag', value: "${GIT_COMMIT[0..7]}")
        ])
      }
    }
  }
}
```

# Retries

When making a commit to a git repository it is possible that between the time of the last pull and the time of the push, the repository has been updated by another process. In this case, the push will fail with a message similar to:

```
error: failed to push some refs to 'your-remote.git'
hint: Updates were rejected because the tip of your current branch is behind its remote counterpart.
```

There is a high risk of this for a repository that is being updated by CI/CD processes especially when it is frequently updated by multiple pipelines. To mitigate this, the Deployment Pipeline will retry the push operation up to a maximum number of times. The number of retries can be configured using the `maximum_retries` parameter. The delay between retries is calculated as 1.5 ^ n seconds, where n is the number of retries attempted. The delay is capped at 30 seconds.
