# SAST Pipeline

The SAST Pipeline template runs Static Application Security Testing (SAST) scans on the application code using SonarQube.

# Parameters

The SAST Pipeline is a [parameterized pipeline](https://www.jenkins.io/doc/book/pipeline/syntax/#parameters) that is intended to be invoked from another pipeline. Some of the parameters can be given default values when an instance of the template is created.

| Parameter Name                 | Pipeline | Template | Description                                                                                                                                              | Default Value                             |
|--------------------------------|----------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| sonarqube_url                  | X        | X        | The URL of the SonarQube server to use when running the SAST scan.                                                                                       | https://sonarqube.cloud.cms.gov           |
| sonarqube_project_key          | X        | X        | The SonarQube project key to use when reporting issues.                                                                                                  |                                           |
| sonarqube_additional_arguments | X        | X        | A JSON serialized array of additional arguments to pass to the SonarQube scanner.                                                                        | []                                        |
| source_path                    | X        | X        | The relative path of the source code to scan.                                                                                                            | .                                         |
| pre_scan_build_command         | X        | X        | A command to run before scanning the source code. Useful if the SAST tool depends on dependencies being installed, or source code being compiled.        |                                           |
| pre_scan_build_image           | X        | X        | The container image to use when running the pre-scan build command.                                                                                      | artifactory.cloud.cms.gov/docker/alpine:3 |
| git_repository                 | X        |          | The URL of the Git repository to clone when running the SAST scan.                                                                                       |                                           |
| git_credentials                | X        |          | The ID of the Jenkins credentials to use when cloning the Git repository.                                                                                |                                           |
| git_commit                     | X        |          | The commit hash or branch name to checkout when running the SAST scan.                                                                                   |                                           |
| git_branch                     | X        |          | The branch that the current build is for (used when reporting results for SonarQube to identify new code and associate detected issues with the branch). |                                           |
| git_change_id                  | X        |          | (Optional) A unique identifier for a pull request (used by SonarQube to track issues detected in new code).                                              |                                           |
| build_retention_days             |      X    | X        | The number of days to retain build logs and artifacts.                                                                                                | 90               |
| build_retention_count            |       X   | X        | The number of builds to retain.                                                                                                                       | 1000             |

### Optional Feature: Snyk Test
| Parameter Name                 | Pipeline | Template | Description                                                                                                                                              | Default Value                             |
|--------------------------------|----------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| enable_snyk_test | X        | X        | Toggle this to enable snyk test. | False |
| snyk_token | X        | X        | Credentials for your snyk organization to enable snyk metrics to appear in the dashboard. |
| snyk_image_tag | X        | X        | The snyk container image tag relevant to your project's language. (python, golang-1.22, php, node, etc.) | alpine |
| snyk_project_name | X        | X        | The snyk project name to appear in the snyk dashboard | my-app |
| snyk_additional_arguments | X        | X        | A JSON serialized array of additional arguments to pass to the Snyk test. | []                                              |
| snyk_additional_env | X        | X        | A JSON serialized array of additional environment variables to pass to the Snyk test.                                          | []                                | 
| vulnerability_severity_threshold | X        | X        | The minimum severity level of vulnerabilities which will cause the build to fail if detected (options: low, medium, high, critical).        | high                                                                | 
| package_file | X        | X        | (Optional) The name of the package file for Snyk to test. |
| package_manager | X        | X        | (Optional) The name of the package manager for Snyk to test. |


# SonarQube Setup

In order to utilize the SAST Pipeline, a project must be created in SonarQube and the project key must be provided as a parameter when running the pipeline. The project key is used to identify the project in SonarQube and to report issues detected by the SAST scan.

1. If your team has not yet been on-boarded to SonarQube, open a CMS Cloud Support Ticket to request access.
2. Log in to SonarQube at [https://sonarqube.cloud.cms.gov](https://sonarqube.cloud.cms.gov).
3. Create a new project in SonarQube.

## SonarQube Project Creation

There are two ways to create a project in SonarQube: manually, or by importing your project from GitHub. When you manually create a project it is important that you use a prefix for the project key that will associate the project with your team or organization. This prefix should be provided to you when you are onboarded. If you import the project from GitHub the project key will be automatically generated based on the repository location (e.g. `<GitHub_Organization>_<GitHub_Repository>`), so you must request that a prefix that matches your GitHub repository be configured for you in SonarQube.

### Create Project Menu

![SonarQube Create Project Menu](../../static/images/SonarQube%20-%20Create%20Project.png)

### Project Settings

![SonarQube Project Settings](../../static/images/SonarQube%20-%20Project%20Settings.png)

### Unanalyzed Project

![SonarQube Unanalyzed Project](../../static/images/SonarQube%20-%20Unanalyzed%20Project.png)

When you first create your project no analysis will have been performed for the `main` branch. Once you run a Jenkins Pipeline that includes the SAST pipeline for your `main` branch, the project dashboard in SonarQube will be populated with the baseline analysis.

### Project Dashboard

![SonarQube Project Dashboard](../../static/images/SonarQube%20-%20Project%20Dashboard.png)

### Branch Switching

![SonarQube Branch Switching](../../static/images/SonarQube%20-%20Branch%20Switching.png)

This menu allows you to view the analysis results for different branches.

## SonarQube Branch Tracking

SonarQube uses a "clean as you code" approach where any issues detected in your `main` or "default" branch will not prevent future pipelines from continuing, however, if you introduce new issues in a branch the SAST scan will fail and the pipeline will not continue. You can then either fix the issues prior to merging the branch, or you can resolve the issues in the SonarQube project dashboard including a reason why the issues can safely be ignored.

# Usage

For general usage see the [CMS ADO Pipeline Catalog README](../../README.md).

The SAST Pipeline is intended to be invoked dynamically from another Pipeline using the [Pipeline: Build Step plugin](https://plugins.jenkins.io/pipeline-build-step/). This allows a project-specific Jenkins pipeline that is responsible for building and testing the application using custom tools and scripts for that purpose, to invoke the standard SAST Pipeline to scan the application source code for code smells and potential defects. There is no specific requirement for how that project-specific pipeline is structured, as long as it can invoke the SAST Pipeline with the required parameters.

```groovy
pipeline {
  stages {
    stage('Testing') {
      parallel {
        // Additional project specific testing stages can be added here and run
        // in parallel with the SAST stage.
        
        stage('SAST') {
          steps {
            script {
              def sastParameters = [
                string(name: 'git_repository', value: "${scm.userRemoteConfigs[0].url}"),
                string(name: 'git_credentials', value: "${scm.userRemoteConfigs[0].credentialsId}"),
                string(name: 'git_commit', value: "${GIT_COMMIT}"),
                string(name: 'git_branch', value: "${env.GIT_BRANCH}"),
                
                // These parameters may be specified here, or as defaults when an instance of the
                // SAST Pipeline is created in the Jenkins dashboard:
                
                // This values corresponds to a project in SonarQube
                string(name: 'sonarqube_project_key', value: 'app-project-key'),
              ]

              if (env.CHANGE_ID) {
                sastParameters.add(string(name: 'git_change_id', value: "${env.CHANGE_ID}"))
              }

              build(job: 'Application SAST', wait: true, propagate: true, parameters: sastParameters)
            }
          }
        }
      }
    }
  }
}
```

For some programming languages, SonarQube requires some additional setup and can be configured with additional language specific parameters. For example, for Java projects, the `sonar.java.binaries` and `sonar.java.libraries` parameters can be used to specify the location of compiled Java classes and libraries that are required for the SAST scan. These parameters can be added to the `sonarqube_additional_arguments` parameter as a JSON serialized array. Necessary setup steps, such as compiling Java classes, can be run using the `pre_scan_build_command` parameter.

```groovy
pipeline {
  stages {
    stage('Test') {
      parallel {
        stage('SAST') {
          steps {
            script {
              def sastParameters = [
                // These parameters leverage the configuration of the Multi-Branch
                // Pipeline so that the Delivery Pipeline runs for the same commit
                // that the project-specific pipeline is running for.
                string(name: 'git_repository', value: "${scm.userRemoteConfigs[0].url}"),
                string(name: 'git_credentials', value: "${scm.userRemoteConfigs[0].credentialsId}"),
                string(name: 'git_commit', value: "${GIT_COMMIT}"),
                string(name: 'git_branch', value: "${env.GIT_BRANCH}"),
                
                // These parameters may be specified here, or as defaults when an instance of the
                // SAST Pipeline is created in the Jenkins dashboard:
                
                // This values corresponds to a project in SonarQube
                string(name: 'sonarqube_project_key', value: 'app-project-key'),
                
                // The values heres are provided as an example that might be used with a Java
                // project. These parameters are optional and may not be needed for all projects.
                string(name: 'sonarqube_additional_arguments', value: '["-Dsonar.java.binaries=./target/classes/"]'),
                string(name: 'pre_scan_build_command', value: 'mvn compile'),
                string(name: 'pre_scan_build_image', value: 'artifactory.cloud.cms.gov/docker/maven:3-amazoncorretto-21'),
              ]

              if (env.CHANGE_ID) {
                sastParameters.add(string(name: 'git_change_id', value: "${env.CHANGE_ID}"))
              }

              build(job: 'App SAST', wait: true, propagate: true, parameters: sastParameters)
            }
          }
        }
      }
    }
  }
}
```

## Copy Artifacts from Jenkins to Sonarqube

For some tech stacks, for example Java Maven, Sonarqube requires the compiled .class files to perform the scan. Copy artifact functionality should be used so needed artifacts will be available for Sonarqube.

1. Add to the pipeline Jenkinsfile, between 'agent' and 'stages':
```
options {
  copyArtifactPermission('*');
}
```

2. Add to the build stage commands:
```
archiveArtifacts artifacts: 'target/**/*.class', allowEmptyArchive: true
```

3. Add to parameters passed into SAST stage:
```
string(name: 'copy_artifacts_job_name', value: "${env.JOB_NAME}"),
string(name: 'copy_artifacts_build_number', value: "${env.BUILD_NUMBER}"),
string(name: 'copy_artifacts_filter', value: 'target/**/*.class')
```
NOTE: The file filter of 'target/**/*.class' is only applicable for Maven projects; this will vary by tech stack.
