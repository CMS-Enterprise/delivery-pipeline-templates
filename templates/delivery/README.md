# Delivery Pipeline

The Delivery Pipeline builds a container image, scans the container image for vulnerabilities as well as malware, and publishes the container image to a registry.

# Parameters

The Delivery Pipeline is a [parameterized pipeline](https://www.jenkins.io/doc/book/pipeline/syntax/#parameters) that is intended to be invoked from another pipeline. Some of the parameters can be given default values when an instance of the template is created.

| Parameter Name                   | Pipeline | Template | Description                                                                                                                                            | Default Value    |
|----------------------------------|----------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|
| image                            | X        | X        | The name of the container image to build and publish.                                                                                                  | docker.io/my-app |
| tag                              | X        | X        | The tag to apply to the container image.                                                                                                               | latest           |
| image_registry_auth_json         |          | X        | The Jenkins Secret File credential to use when authenticating with the container registry.                                                             |                  |
| image_push_enabled               | X        | X        | Enable the image push step.                                                                                                                            | false            |
| update_latest                    | X        | X        | When true, update the latest tag to point to the newly built image.                                                                                    | false            |
| build_args                       | X        | X        | Additional arguments to pass to the `docker build` command.                                                                                            |                  |
| build_dir                        | X        | X        | The context directory to use when building the container image.                                                                                        | .                |
| dockerfile                       | X        | X        | The path to the Dockerfile to use when building the container image.                                                                                   | Dockerfile       |
| build_target                     | X        | X        | The build stage to target when building the container image.                                                                                           |                  |
| enable_cache                     | X        | X        | Enable Kaniko image build cache.                                                                                                                       | false            |
| enable_ansi_colors               |          | X        | Enable ANSI color output in the Jenkins console (requires AnsiColor plugin).                                                                           | true             |
| git_repository                   | X        |          | The URL of the Git repository to clone when building the container image.                                                                              |                  |
| git_credentials                  | X        |          | The ID of the Jenkins credentials to use when cloning the Git repository                                                                               |                  |
| git_commit                       | X        |          | The commit hash or branch name to checkout when building the container image.                                                                          |                  |
| log_level                        | X        |          | The log level to use for the container build process.                                                                                                  | info             |
| build_memory_limit               | X        | X        | Memory limit for the image build process.                                                                                                              | 1Gi              |
| copy_artifacts_job_name          | X        |          | The Jenkins job name from which to copy artifacts.                                                                                                     |                  |
| copy_artifacts_build_number      | X        |          | The Jenkins job build number from which to copy artifacts.                                                                                             |                  |
| copy_artifacts_filter            | X        |          | A string expression to filter artifact names.                                                                                                          |                  |
| snyk_token                       |          | X        | The Snyk API token to use when scanning the container image.                                                                                           |                  |
| snyk_project_name                | X        | X        | The name of the Snyk project to associate the container image scan with.                                                                               | my-app           |
| vulnerability_severity_threshold | X        | X        | The minimum severity level of vulnerabilities which will cause the build to fail if detected (options: low, medium, high, critical).                   | high             |
| continue_on_image_scan_failure   | X        | X        | When set to true, the pipeline will continue to the image publish step even if either of the malware scan or vulnerability scan fails for any reason.  | false            |
| build_retention_days             |          | X        | The number of days to retain build logs and artifacts.                                                                                                 | 90               |
| build_retention_count            |          | X        | The number of builds to retain.                                                                                                                        | 1000             |

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

## Image Registry Authentication

The Delivery Pipeline requires a Jenkins Secret File credential to authenticate with the container registry. The same credential is used both for pulling images from the registry during the container build (for Dockerfiles using a private base image), and for publishing the resulting container image. The credential should be created in Jenkins and the ID of the credential should be specified in the Delivery Pipeline configuration as the "JSON File Docker Config for Authentication (Secret File)" parameter. The credential should be a JSON file with the following format:

```json
{
  "auths": {
    "https://index.docker.io/v1/": {
      "auth": "base64-encoded-username-and-password"
    }
  }
}
```

Note: when generating this file, it is important that the base64 encoded username and password not contain a trailing space. Here is a sample command to generate the auth token:

```bash
echo -n "$username:$password" | base64
```

If images are pulled from multiple container registries, then multiple entries can be included in the `auths` object.

## Snyk Integration

When enabling Snyk, an Organization must exist in Snyk and the Snyk API token must have access to that Organization. The Snyk API token should be stored in Jenkins as a secret text credential. The Snyk API token can be created in the Snyk UI under the user settings. The Snyk API token should be stored in Jenkins as a secret text credential.

Findings reported to Snyk will be associated with the specified Project name, and will have a target name based on the name of the container (forward slashes are replaced by underscores). The Snyk project name should be unique within the Snyk Organization.

![](../../static/images/Snyk%20Projects%20Dashboard.png)

When a container scan results are reported to Snyk, the results are associated with the tag being scanned. However, in the Snyk dashbard each project automatically displays only the latest scan results. The results for individual scans can be found in the history view for the Snyk project, however the easies way to find results for a specific build of a container image is to look for the Snyk URL in the Jenkins console output for the Delivery Pipeline.

![](../../static/images/Vulnerability%20Scan%20Console%20Output.png)

## Delivery Pipeline Artifacts

The Delivery Pipeline produces the following artifacts when Snyk and/or ClamAV are enabled respectively: 

 * `snyk-vulnerabilities.json`
 * `snyk-sbom.json`
 * `virus-report.clamav.txt`

## Build Retention

The Delivery Pipeline can be configured to retain build logs and artifacts for a specified number of days and/or a specified number of builds. The default values are 90 days and 1000 builds respectively. The build retention settings can be configured when an instance of the Delivery Pipeline is created in the Jenkins dashboard.
