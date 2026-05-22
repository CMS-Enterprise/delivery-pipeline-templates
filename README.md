# CMS ADO Pipeline Catalog

**_This is an attempt at reimplementation of a pipeline template catalog for common CMS technologies when playing around with AI._**

**_This is not to be used for anything at this point. It contains untested AI generated code._**
# Delivery Pipeline Templates

A CloudBees Pipeline Template Catalog providing standardized CI/CD pipelines for containerized applications.

## Templates

| Template | Type | Description |
|----------|------|-------------|
| **web-app** | MULTIBRANCH | Build, test, scan, and deploy web applications through dev/staging/prod |
| **multi-branch** | MULTIBRANCH | Branch-aware pipeline (feature/develop/release/hotfix/main) |
| **jfrog-secure** | MULTIBRANCH | JFrog Artifactory + Xray gated delivery with Snyk SAST |
| **library-publish** | MULTIBRANCH | Build, test, and publish internal packages (npm/Maven) |
| **credential-rotation** | SCRIPTED | Scheduled credential rotation from AWS Secrets Manager |
| **renovate** | SCRIPTED | Automated dependency and image pin updates |

## Structure

```
catalog.yaml                    Root catalog definition
templates/
  <name>/
    template.yaml               Parameters and metadata
    Jenkinsfile                  Pipeline logic
vars/                           Shared library steps
resources/pods/                 Reusable Kubernetes pod specs
internal/
  template.schema.json          JSON Schema for template.yaml validation
```

## Shared Library Steps (vars/)

These steps are available to all templates when this repo is configured as a Jenkins Shared Library:

| Step | Description |
|------|-------------|
| `buildGradle` | Gradle build in ephemeral pod |
| `buildNpm` | npm ci + build in ephemeral pod |
| `unitTestGradle` | Gradle test execution |
| `unitTestNpm` | npm test with coverage |
| `dockerBuild` | Podman build + push to registry |
| `jfrogBuild` | Podman build + push to JFrog Artifactory |
| `jfrogScan` | JFrog Xray build scan |
| `jfrogPromote` | Promote build from staging to prod repo |
| `trivyScan` | Trivy container vulnerability scan |
| `snykCodeScan` | Snyk Code SAST scan |
| `snykDependencyScan` | Snyk Open Source SCA scan |
| `deployToK8s` | kubectl rollout to Kubernetes |
| `rollbackK8s` | kubectl rollout undo |
| `smokeTest` | HTTP health check with retries |
| `slackNotify` | Slack webhook notification |
| `cosignSign` | Sign container image with cosign + AWS KMS |
| `cosignVerify` | Verify container image signature |
| `awsAssumeRole` | AWS STS AssumeRole |

## Usage

### 1. Add the Catalog

In CloudBees CI Operations Center or Controller:
- Navigate to **Manage Jenkins > Pipeline Template Catalogs**
- Add a new catalog pointing to this repository
- The catalog will appear as "Delivery Pipeline Templates"

### 2. Create a Job from a Template

- **New Item > From Template** and select a template
- Fill in the parameters (registry, image name, credentials, etc.)
- For MULTIBRANCH templates, configure the branch source

### 3. Configure as Shared Library

To use the `vars/` steps, add this repository as a **Global Pipeline Library**:
- Name: `delivery-pipeline-templates`
- Default version: `main`
- Source: Git pointing to this repository

## Migration from JTE

| JTE Concept | PTC Equivalent |
|-------------|----------------|
| `pipeline_config.groovy` (org) | `catalog.yaml` (static metadata only) |
| `pipeline_config.groovy` (team) | Template parameters (set at job creation) |
| `libraries { lib { param = val } }` | `vars/` shared library + template parameters |
| `library_config.groovy` fields | `template.yaml` parameters section |
| `template_methods {}` | Steps called directly in Jenkinsfile |
| `on_pull_request` / `on_merge` | Branch conditionals in Jenkinsfile |
| JTE governance tiers | CloudBees RBAC + template catalog access control |
