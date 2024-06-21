# Delivery Pipeline

The Delivery Pipeline builds a container image, scans the container image for vulnerabilities as well as malware, and publishes the container image to a registry.

# Parameters

The Delivery Pipeline is a [parameterized pipeline](https://www.jenkins.io/doc/book/pipeline/syntax/#parameters) that is intended to be invoked from another pipeline. Some of the parameters can be given default values when an instance of the template is created.

| Parameter Name       | Pipeline | Template | Description                                                                     | Default Value    |
|----------------------|----------|----------|---------------------------------------------------------------------------------|------------------|
| image                | X        | X        | The name of the container image to build and publish.                           | docker.io/my-app |
| tag                  | X        | X        | The tag to apply to the container image.                                        | latest           |
| registry_credentials |          | X        | The Jenkins credentials to use when authenticating with the container registry. |                  |
| build_args           | X        | X        | Additional arguments to pass to the `docker build` command.                     |                  |
| build_dir            | X        | X        | The context directory to use when building the container image.                 | .                |
| dockerfile           | X        | X        | The path to the Dockerfile to use when building the container image.            | Dockerfile       |
| build_target         | X        | X        | The build stage to target when building the container image.                    |                  |
| platform             | X        | X        | A comma separated list of platforms to build the container image for.           | linux/amd64      |
| enable_ansi_colors   |          | X        | Enable ANSI color output in the Jenkins console (requires AnsiColor plugin).    | true             |
| git_repository       | X        |          | The URL of the Git repository to clone when building the container image.       |                  |
| git_credentials      | X        |          | The ID of the Jenkins credentials to use when cloning the Git repository        |                  |
| git_commit           | X        |          | The commit hash or branch name to checkout when building the container image.   |                  |
| log_level            | X        |          | The log level to use for the container build process.                           | info             |

# Usage

For general usage see the [CMS ADO Pipeline Catalog README](../../README.md).

The Delivery Pipeline is intended to be invoked dynamically from another Pipeline using the [Pipeline: Build Step plugin](https://plugins.jenkins.io/pipeline-build-step/). This allows a project-specific Jenkins pipeline that is responsible for building and testing the application, using custom tools and scripts for that purpose, to invoke the standard Delivery Pipeline to subsequently build and publish the container image. There is no specific requirement for how that project-specific pipeline is structured, as long as it can invoke the Delivery Pipeline with the required parameters. As an example, a Jenkinsfile that uses the Build Step could be checked in to a repository and a [Multi-Branch Pipeline](https://www.jenkins.io/doc/book/pipeline/multibranch/) could be configured for that repository.

```groovy
pipeline {
  agent {
    // The Pod specification here is specific to the project being build and
    // does not affect the Delivery Pipeline.
    kubernetes {
      yaml """
      apiVersion: v1
      kind: Pod
      spec:
        restartPolicy: Never
        containers:
        - name: build
          image: docker/node:18
          command: ['tail', '-f', '/dev/null']
      """
    }
  }

  stages {
    // These steps are just examples and would be specific to the project
    stage('Build') {
      steps {
        container('build') {
          dir('node-server') {
            sh 'npm ci'
          }
        }
      }
    }

    stage ('Test') {
      parallel {
        stage('Unit Test') {
          steps {
            container('build') {
              dir('node-server') {
                sh 'npm run test:unit'
              }
            }
          }
        }
        stage('Lint') {
          steps {
            container('build') {
              dir('node-server') {
                sh 'npm run lint'
              }
            }
          }
        }
      }
    }

    stage('Delivery') {
      steps {
        // The job name here corresponds to the name assigned to the instance of the Delivery Pipeline created in Jenkins from the Delivery Pipeline Template.
        build(job: 'Node Server Delivery', wait: true, propagate: true, parameters: [
          string(name: 'tag', value: "${GIT_COMMIT[0..7]}"),
          
          // These parameters leverage the conifugration of the Multi-Branch
          // Pipeline so that the Delivery Pipeline runs for the same commit
          // that the project-specific pipeline is running for.
          string(name: 'git_repository', value: "${scm.userRemoteConfigs[0].url}"),
          string(name: 'git_credentials', value: "${scm.userRemoteConfigs[0].credentialsId}"),
          string(name: 'git_commit', value: "${GIT_COMMIT}"),
        ])
      }
    }
  }
}

```
