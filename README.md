# CMS ADO Pipeline Catalog

This repository contains a Jenkins [Pipeline Template Catalog](https://docs.cloudbees.com/docs/cloudbees-ci/latest/pipeline-templates-user-guide/) for Application Development Organizations to use to build, scan, publish, and deploy containerized applications at CMS.

This resource was co developed with other IUSG cloud engineering contractors such as Leidos and Samtek.

# Pipeline Templates

The following pipeline templates are available in this catalog:

* [SAST Scan](https://github.com/CMS-Enterprise/delivery-pipeline-templates/blob/419aec7ae2da320b57a139d8331c96015f08708a/templates/sast/README.md) - Runs Static Application Security Testing (SAST) scans on the application code (using SonarQube).
* [Delivery Pipeline](https://github.com/CMS-Enterprise/delivery-pipeline-templates/blob/419aec7ae2da320b57a139d8331c96015f08708a/templates/delivery/README.md) - Runs container build and scan steps and publishes the resulting container image to the CMS container registry (Artifactory).
* [GitOps Deployment](https://github.com/CMS-Enterprise/delivery-pipeline-templates/blob/419aec7ae2da320b57a139d8331c96015f08708a/templates/deployment/README.md) - Runs steps to deploy the container image to a Kubernetes cluster by updating the image tag in Kubernetes manifests.

# Architecture

The pipeline templates in this catalog are designed to be used by invoking them from another Jenkins Pipeline that will typically be a Multi-Branch Pipeline that runs when the source code of a project changes, and/or can run with different configurations for different branches.

![](https://github.com/CMS-Enterprise/delivery-pipeline-templates/blob/419aec7ae2da320b57a139d8331c96015f08708a/static/images/Jenkins%20Delivery%20Pipelines%20-%20Architecture.png)

![](https://github.com/CMS-Enterprise/delivery-pipeline-templates/blob/419aec7ae2da320b57a139d8331c96015f08708a/static/images/Jenkins%20Delivery%20Pipelines%20-%20Sequence.png)

## Tools

| Stage      | Step                        | Tool                                            |
|------------|-----------------------------|-------------------------------------------------|
| Build      | Build                       | Application Specific (e.g. Maven, npm, pip)     |
| Test       | Unit Test                   | Application Specific (e.g. JUnit, Jest, Pytest) |
| Test       | Lint                        | Application Specific (e.g. ESLint, Pylint)      |
| SAST       | Scan Source                 | SonarQube                                       |
| SAST       | Scan Dependencies(Optional) | Snyk                                            |
| Delivery   | Build Image                 | Kaniko                                          |
| Delivery   | Vulnerability Scan          | Snyk                                            |
| Delivery   | Malware Scan                | ClamAV                                          |
| Delivery   | Publish Image               | Crane -> Artifactory                            |
| Deployment | Update Image Tags           | Kustomize                                       |
| Deployment | Commit & Push               | Git -> ArgoCD                                   |

# Delivery & Deployment Model

The delivery and deployment model for the pipeline templates in this catalog is based on the GitOps model. The GitOps model is a way to do Continuous Deployment where the desired system state is versioned in a Git repository. The Git repository in this case is a Kubernetes manifest repository that contains the desired state of the Kubernetes cluster. The GitOps model is implemented using ArgoCD, which is a declarative, GitOps continuous delivery tool for Kubernetes.

As code changes are made for an application, the Delivery pipeline can be used to continuously build the latest container image for the application. The resulting container images can be pushed to the CMS container registry (Artifactory). The Delivery pipeline is intended to be run after basic application testing and SAST scanning has been completed. The Delivery pipeline includes steps to scan the resulting container image for vulnerabilities and malware before publishing the image to the container registry. With a container image published to the container registry, it can be leveraged by developers for local testing, or it can be deployed to a Kubernetes cluster using the Deployment pipeline and ArgoCD.

The Deployment pipeline updates references to the container image in the Kubernetes manifests in the manifests repository, and then commits and pushes the changes to the repository. The Deployment pipeline can be configured to target a specific branch and a specific subfolder of the manifests repository, thus allowing for different deployment strategies for different environments, as well as enabling feature branch testing. ArgoCD is configured to watch the manifests repository for changes, and will automatically deploy the updated manifests to the Kubernetes cluster. The Deployment pipeline is intended to be run after the Delivery pipeline has successfully published a container image to the container registry.

# Usage

To use the Pipeline Templates in this catalog, the [CloudBees Pipeline: Templates](https://docs.cloudbees.com/plugins/ci/cloudbees-workflow-template) plugin must be installed and enabled. To make the Pipeline Templates available to your Jenkins instance, perform the following steps:

## Add the Pipeline Templates Catalog

From the Jenkins dashboard, select "Pipeline Template Catalogs" and then click the "Add catalog" link under that.

![The Pipeline Template Catalogs Menu](https://github.com/CMS-Enterprise/delivery-pipeline-templates/blob/419aec7ae2da320b57a139d8331c96015f08708a/static/images/Pipeline%20Template%20Catalogs%20-%20Add.png)

Select the Branch or Tag that you want to utilize. Selecting the `main` branch will give you the latest version of the templates, however it could lead to breaking changes being automatically introduced during a future update. Selecting a "stable" version branch such as `v1` will give you a stable version of the templates that will not introduce breaking changes, but will be updated with bug and security fixes. Selecting a specific version tag such as `v1.0.0` will give you a specific version of the templates that will not be updated.

![New Pipeline Catalog Source Control Options](https://github.com/CMS-Enterprise/delivery-pipeline-templates/blob/419aec7ae2da320b57a139d8331c96015f08708a/static/images/Catalog%20Source%20Control%20Options.png)

Enter the URL of this repository (`https://github.com/CMS-Enterprise/delivery-pipeline-templates.git`) in the "Catalog source code repository location" section, and click Save.

## Create an Instance of a Pipeline Template

In order to utilize the Pipeline Templates in this catalog, you must create an instance of the template in your Jenkins instance. To do this, click the "New Item" button from the Jenkins dashboard, and select the desired template name: "SAST Scan", "Delivery Pipeline", or "Deployment". Enter a name for the pipeline instance, and click OK.

Once the new Pipeline has been created, you will be prompted to select the parameters for the pipeline. The parameters are specific to each template, and are documented in the README for each template. Specifying parameter values when creating an instance of the template is optional and the specified values only serve as defaults for the parameters when the pipeline is run.

## Using an Instance of a Pipeline Template

Instances of these Pipeline Templates are not intended to be run manually (although you can do so by clicking the "Build With Parameters" button in Jenkins). Instead, they are intended to be run from another Jenkins Pipeline using the [Pipeline: Build Step plugin](https://plugins.jenkins.io/pipeline-build-step/). This allows a project-specific Jenkins pipeline that is responsible for building and testing the application, using custom tools and scripts for that purpose, to invoke the these Pipelines for steps that are common to multiple applications. For details on how to invoke an instance of one of these Pipeline Templates see the specific template README.

## Pipeline Organization Strategy

If your project contains multiple applications or services, you can organize your Delivery Pipelines in one of two ways: create separate pipelines for each of your projects that have different default parameter values for that specific application, or create a single pipeline with minimal default settings, that can be invoked from multiple application specific pipelines with different parameter values. The former strategy will result in more pipelines appearing in your Jenkins dashboard, but may make it easier to find a specific pipeline run for a particular application.

For example, if you use a single Delivery pipeline to build the container image for multiple applications, passing the different image name and other parameters from the triggering Jenkins files, this build history for the shared Delivery pipeline will include a mix of pipeline runs for both applications. This can make it more difficult to determine when one application or another is having pipeline failures.
