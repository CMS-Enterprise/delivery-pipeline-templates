# Delivery Pipeline

The Delivery Pipeline builds a container image, scans the container image for vulnerabilities as well as malware, and publishes the container image to a registry.

# Parameters

The Delivery Pipeline is a [parameterized pipeline](https://www.jenkins.io/doc/book/pipeline/syntax/#parameters) that is intended to be invoked from another pipeline. Some of the parameters can be given default values when an instance of the template is created.

| Parameter Name              | Pipeline | Template | Description                                                                                                                                    | Default Value    |
|-----------------------------|----------|----------|------------------------------------------------------------------------------------------------------------------------------------------------|------------------|
| image                       | X        | X        | The name of the container image to build and publish.                                                                                          | docker.io/my-app |
| tag                         | X        | X        | The tag to apply to the container image.                                                                                                       | latest           |
| registry_credentials        |          | X        | The Jenkins credentials to use when authenticating with the container registry.                                                                |                  |
| build_args                  | X        | X        | Additional arguments to pass to the `docker build` command.                                                                                    |                  |
| build_dir                   | X        | X        | The context directory to use when building the container image.                                                                                | .                |
| dockerfile                  | X        | X        | The path to the Dockerfile to use when building the container image.                                                                           | Dockerfile       |
| build_target                | X        | X        | The build stage to target when building the container image.                                                                                   |                  |
| platform                    | X        | X        | A comma separated list of platforms to build the container image for.                                                                          | linux/amd64      |
| enable_ansi_colors          |          | X        | Enable ANSI color output in the Jenkins console (requires AnsiColor plugin).                                                                   | true             |
| git_repository              | X        |          | The URL of the Git repository to clone when building the container image.                                                                      |                  |
| git_credentials             | X        |          | The ID of the Jenkins credentials to use when cloning the Git repository                                                                       |                  |
| git_commit                  | X        |          | The commit hash or branch name to checkout when building the container image.                                                                  |                  |
| log_level                   | X        |          | The log level to use for the container build process.                                                                                          | info             |
| kaniko_memory_limit         | X        | X        | Kaniko memory limit input for those larger builds.                                                                                             | 1Gi              |
| copy_artifacts_job_name     | X        |          | The Jenkins job name from which to copy artifacts.                                                                                             |                  |
| copy_artifacts_build_number | X        |          | The Jenkins job build number from which to copy artifacts.                                                                                     |                  |
| copy_artifacts_filter       | X        |          | A string expression to filter artifact names.                                                                                                  |                  |
| enable_cache                | X        |          | Enable Kaniko image build cache.                                                                                                               | false            |
| enable_compressed_caching   | X        |          | Enable Kaniko tar compression for cached layers. Note: This decreases build runtimes but increases memory usage (especially for large builds). | false            |

# Usage

For general usage see the [CMS ADO Pipeline Catalog README](../../README.md).

The Delivery Pipeline is intended to be invoked dynamically from another Pipeline using the [Pipeline: Build Step plugin](https://plugins.jenkins.io/pipeline-build-step/). This allows a project-specific Jenkins pipeline that is responsible for building and testing the application, using custom tools and scripts for that purpose, to invoke the standard Delivery Pipeline to subsequently build and publish the container image. There is no specific requirement for how that project-specific pipeline is structured, as long as it can invoke the Delivery Pipeline with the required parameters. As an example, a Jenkinsfile that uses the Build Step could be checked in to a repository and a [Multi-Branch Pipeline](https://www.jenkins.io/doc/book/pipeline/multibranch/) could be configured for that repository.

```groovy
pipeline {
  stages {
    // Stages and steps that are specific to the project should be specified
    // before the Delivery stage

    stage('Delivery') {
      steps {
        // The job name here corresponds to the name assigned to the instance of the Delivery
        // Pipeline created in Jenkins from the Delivery Pipeline Template.
        build(job: 'App Delivery', wait: true, propagate: true, parameters: [
          string(name: 'tag', value: "${GIT_COMMIT[0..7]}"),
          
          // These parameters leverage the configuration of the Multi-Branch
          // Pipeline so that the Delivery Pipeline runs for the same commit
          // that the project-specific pipeline is running for.
          string(name: 'git_repository', value: "${scm.userRemoteConfigs[0].url}"),
          string(name: 'git_credentials', value: "${scm.userRemoteConfigs[0].credentialsId}"),
          string(name: 'git_commit', value: "${GIT_COMMIT}"),
          
          // These parameters may be specified here, or as defaults when an instance of the
          // SAST Pipeline is created in the Jenkins dashboard
          
          string(name: 'image', value: "docker.io/your-account/your-app"),
          // buid_args specified here corresond to ARG statements in your Dockerfile
          string(name: 'build_args', value: "{ \"BUILD_REVISION\": \"${GIT_COMMIT}\" }"),
        ])
      }
    }
  }
}
```

## Dockerfile Considerations and Use of Copy Artifact

Typically, the Dockerfile for an ADO is self-contained and should build and package the software. For some ADOs, the Dockerfile may simply assume that build artifacts have already been compiled and try to copy them in. For this scenario, the following should be added to the project Jenkinsfile so the artifacts will be archived and relevant data on the artifacts passed into the delivery pipeline:

1. Add to the pipeline Jenkinsfile, between 'agent' and 'stages':
```
options {
  copyArtifactPermission('*');
}
```

2. Add to the build stage commands:
```
archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
```

3. Add to parameters passed into delivery stage:
```
string(name: 'copy_artifacts_job_name', value: "${env.JOB_NAME}"),
string(name: 'copy_artifacts_build_number', value: "${env.BUILD_NUMBER}"),
string(name: 'copy_artifacts_filter', value: '**/target/*.jar')
```
NOTE: The file filter of '**/target/*.jar' is only applicable for Maven projects; this will vary by tech stack.
