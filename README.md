# CMS ADO Pipeline Catalog

This repository contains a Jenkins [Pipeline Template Catalog](https://docs.cloudbees.com/docs/cloudbees-ci/latest/pipeline-templates-user-guide/) for Application Development Organizations to use to build, scan, publish, and deploy containerized applications at CMS.

# Pipeline Templates

The following pipeline templates are available in this catalog:

* [SAST Scan](./templates/sast/README.md) - Runs Static Application Security Testing (SAST) scans on the application code (using SonarQube).
* [Delivery Pipeline](./templates/delivery/README.md) - Runs container build and scan steps and publishes the resulting container image to the CMS container registry (Artifactory).
* [Deployment](./templates/deployment/README.md) - Runs steps to deploy the container image to a Kubernetes cluster by updating the image tag in Kubernetes manifests.
