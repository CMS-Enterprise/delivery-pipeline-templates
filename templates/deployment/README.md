# Deployment Pipeline

The Deployment Pipeline clones a Git repository containing Kubernetes manifests, updates the manifests with a new container image, and pushes the changes back to the repository.

# Parameters

The Deployment Pipeline is a [parameterized pipeline](https://www.jenkins.io/doc/book/pipeline/syntax/#parameters) that is intended to be invoked from another pipeline. Some of the parameters can be given default values when an instance of the template is created.

| Parameter Name       | Pipeline | Template | Description                                                                 | Default Value          |
|----------------------|----------|----------|-----------------------------------------------------------------------------|------------------------|
| git_repository       | X        | X        | The URL of the Git repository containing Kubernetes manifests.              |                        |
| git_branch           | X        | X        | The branch name to checkout.                                                | main                   |
| git_credentials      | X        | X        | Jenkins credential ID for accessing the Git repository.                     |                        |
| git_user_email       | X        | X        | The email address to use for git commits.                                   |                        |
| environment_path     | X        | X        | Path to the environment directory in the manifest repository.               | ${default_environment_path} |
| target_service       | X        | X        | The name of the service to update.                                          | ${default_target_service}    |
| image                | X        | X        | The fully qualified container image name (including the registry).          | ${default_image}       |
| tag                  | X        |          | The tag of the container image.                                             |                        |

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
          string(name: 'git_repository', value: "${scm.userRemoteConfigs[0].url}"),

          string(name: 'git_branch', value: "main"),
          string(name: 'git_credentials', value: "${scm.userRemoteConfigs[0].credentialsId}"),
          string(name: 'git_user_email', value: "your-email@example.com"),
          string(name: 'environment_path', value: 'dev'),
          string(name: 'target_service', value: 'my-service'),
          string(name: 'image', value: "artifactory.cloud.cms.gov/your-account/your-app:${GIT_COMMIT[0..7]}"),
          string(name: 'tag', value: "${GIT_COMMIT[0..7]}")
        ])
      }
    }
  }
}