# CMS ADO Pipeline Catalog

This repository contains a Jenkins [Pipeline Template Catalog](https://docs.cloudbees.com/docs/cloudbees-ci/latest/pipeline-templates-user-guide/) for Application Development Organizations to use to build, scan, publish, and deploy containerized applications at CMS.

# Pipeline Templates

The following pipeline templates are available in this catalog:

* [SAST Scan](./templates/sast/README.md) - Runs Static Application Security Testing (SAST) scans on the application code (using SonarQube).
* [Delivery Pipeline](./templates/delivery/README.md) - Runs container build and scan steps and publishes the resulting container image to the CMS container registry (Artifactory).
* [Deployment](./templates/deployment/README.md) - Runs steps to deploy the container image to a Kubernetes cluster by updating the image tag in Kubernetes manifests.

# Architecture

The pipeline templates in this catalog are designed to be used by invoking the from another Jenkins Pipeline that will typically be a Multi-Branch Pipeline that runs when the source code of a project changes, and/or can run with different configurations for different branches.

![](./static/images/Jenkins%20Delivery%20Pipelines%20-%20Architecture.png)

![](./static/images/Jenkins%20Delivery%20Pipelines%20-%20Sequence.png)

# Usage

To use the Pipeline Templates in this catalog, the [CloudBees Pipeline: Templates](https://docs.cloudbees.com/plugins/ci/cloudbees-workflow-template) plugin must be installed and enabled. To make the Pipeline Templates available to your Jenkins instance, perform the following steps:

## Add the Pipeline Templates Catalog

From the Jenkins dashboard, select "Pipeline Template Catalogs" and then click the "Add catalog" link under that.

![The Pipeline Template Catalogs Menu](./static/images/Pipeline%20Template%20Catalogs%20-%20Add.png)

Select the Branch or Tag that you want to utilize. Selecting the `main` branch will give you the latest version of the templates, however it could lead to breaking changes being automatically introduced during a future update. Selecting a "stable" version branch such as `v1` will give you a stable version of the templates that will not introduce breaking changes, but will be updated with bug and security fixes. Selecting a specific version tag such as `v1.0.0` will give you a specific version of the templates that will not be updated.

![New Pipeline Catalog Source Control Options](./static/images/Catalog%20Source%20Control%20Options.png)

Enter the URL of this repository (`https://github.com/CMS-Enterprise/delivery-pipeline-templates.git`) in the "Catalog source code repository location" section, and click Save.

## Create an Instance of a Pipeline Template

In order to utilize the Pipeline Templates in this catalog, you must create an instance of the template in your Jenkins instance. To do this, click the "New Item" button from the Jenkins dashboard, and select the desired template name: "SAST Scan", "Delivery Pipeline", or "Deployment". Enter a name for the pipeline instance, and click OK.

Once the new Pipeline has been created, you will be prompted to select the parameters for the pipeline. The parameters are specific to each template, and are documented in the README for each template. Specifying parameter values when creating an instance of the template is optional and the specified values only serve as defaults for the parameters when the pipeline is run.

## Pipeline Organization Strategy for Monorepos

If your repository or project contains multiple applications or services, you can organize your Delivery Pipelines in one of two ways: create separate pipelines for each of your projects that have different default parameter values for that specific application, or create a single pipeline with minimal default settings, that can be invoked from multiple application specific pipelines with different parameter values. The former strategy will result in more pipelines appearing in your Jenkins dashboard, but may make it easier to find a specific pipeline run for a particular application.
