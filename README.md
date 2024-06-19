# CMS ADO Pipeline Catalog

This repository contains a Jenkins [Pipeline Template Catalog](https://docs.cloudbees.com/docs/cloudbees-ci/latest/pipeline-templates-user-guide/) for Application Development Organizations to use to build, scan, publish, and deploy containerized applications at CMS.

# Pipeline Templates

The following pipeline templates are available in this catalog:

* [SAST Scan](./templates/sast/README.md) - Runs Static Application Security Testing (SAST) scans on the application code (using SonarQube).
* [Delivery Pipeline](./templates/delivery/README.md) - Runs container build and scan steps and publishes the resulting container image to the CMS container registry (Artifactory).
* [Deployment](./templates/deployment/README.md) - Runs steps to deploy the container image to a Kubernetes cluster by updating the image tag in Kubernetes manifests.

# Usage

To use the Pipeline Templates in this catalog, the [CloudBees Pipeline: Templates](https://docs.cloudbees.com/plugins/ci/cloudbees-workflow-template) plugin must be installed and enabled. To make the Pipeline Templates available to your Jenkins instance, perform the following steps:

## Add the Pipeline Templates Catalog

From the Jenkins dashboard, select "Pipeline Template Catalog" and then click the "Add catalog" link under that.

Select the Branch or Tag that you want to utilize. Selecting the `main` branch will give you the latest version of the templates, however it could lead to breaking changes being automatically introduced during a future update. Selecting a "stable" version branch such as `v1` will give you a stable version of the templates that will not introduce breaking changes, but will be updated with bug and security fixes. Selecting a specific version tag such as `v1.0.0` will give you a specific version of the templates that will not be updated.

Enter the URL of this repository (`https://github.com/CMS-Enterprise/delivery-pipeline-templates.git`) in the "Catalog source code repository location" section, and click Save.

## Create an Instance of a Pipeline Template

In order to utilize the Pipeline Templates in this catalog, you must create an instance of the template in your Jenkins instance. To do this, click the "New Item" button from the Jenkins dashboard, and select the desired template name:  
