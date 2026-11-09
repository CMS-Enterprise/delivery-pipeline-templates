# CMS ADO Pipeline Catalog

**_This is an attempt at reimplementation of a pipeline template catalog for common CMS technologies when playing around with AI and I needed a space to put things._**

**_This is not to be used for anything at this point. It contains untested AI generated code._**

TODO:

- demo: post always archiveArtifacts

# Delivery Pipeline Templates

A CloudBees Pipeline Template Catalog providing standardized CI/CD pipelines for containerized applications.

## Templates

| Template                | Type        | Description                                                             |
| ----------------------- | ----------- | ----------------------------------------------------------------------- |
| **web-app**             | MULTIBRANCH | Build, test, scan, and deploy web applications through dev/staging/prod |
| **multi-branch**        | MULTIBRANCH | Branch-aware pipeline (feature/develop/release/hotfix/main)             |
| **jfrog-secure**        | MULTIBRANCH | JFrog Artifactory + Xray gated delivery with Snyk SAST                  |
| **library-publish**     | MULTIBRANCH | Build, test, and publish internal packages (npm/Maven)                  |
| **credential-rotation** | SCRIPTED    | Scheduled credential rotation from AWS Secrets Manager                  |
| **renovate**            | SCRIPTED    | Automated dependency and image pin updates                              |

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

These steps are available to all templates when this repo is configured as a
Jenkins Shared Library. `demos/Jenkinsfile` is the integration test for them;
the tables below record what it has exercised. Draft as of demo build 288
(2026-11-09) — update a row when its status changes.

### Tested successfully in the demo pipeline

| Step | Description | Exercised by |
| ---- | ----------- | ------------ |
| `changedServices` | Select which monorepo services a build needs | Fetch Source |
| `eslintNextLint` | ESLint for Next.js | Lint: nextjs |
| `eslintVueLint` | ESLint for Vue | Lint: vue |
| `eslintSvelteLint` | ESLint for SvelteKit | Lint: sveltekit |
| `pylintLint` | Pylint | Lint: django |
| `golangciLint` | golangci-lint | Lint: go |
| `checkstyleLintMaven` | Checkstyle via Maven | Lint: maven |
| `checkstyleLint` | Checkstyle via Gradle | Lint: gradle |
| `hadolintLint` | Dockerfile lint | Lint (every service) |
| `shellcheckLint` | Shell script lint | Lint |
| `trufflehogScan` | Secret scan | Source Scans |
| `clamavScan` | Malware scan of source and images | Source Scans, Image Scans |
| `auditNpm` | npm audit gate | Source Scans |
| `opaPolicyScan` | Conftest/OPA policy check of pod templates | Source Scans |
| `trivyIacScan` | Trivy misconfiguration scan | Source Scans |
| `tflintLint` | TFLint on Terraform code | Source Scans (`demos/terraform`) |
| `buildNpm` | npm ci + build in ephemeral pod | Build: nextjs, vue, sveltekit |
| `buildGo` | Go build | Build: go |
| `buildMaven` | Maven build | Build: maven |
| `buildGradle` | Gradle build | Build: gradle |
| `testNpm` | npm type check + test | Test: vue, sveltekit |
| `jestTest` | Jest with junit/coverage | Test: nextjs |
| `pytestTest` | pytest with coverage | Test: django |
| `testGo` | go test | Test: go |
| `unitTestMaven` | Maven unit tests | Test: maven |
| `unitTestGradle` | Gradle unit tests | Test: gradle |
| `cucumberTest` | Cucumber scenarios via Gradle | Test: gradle |
| `jfrogBuildPublish` | Podman build + push + sign, one image per call | Build and Publish Images |
| `jfrogScan` | JFrog Xray build scan | Image Scans and SBOMs |
| `trivyScan` | Trivy container vulnerability scan | Image Scans and SBOMs |
| `grypeScan` | Grype container vulnerability scan | Image Scans and SBOMs |
| `jfrogSbom` | CycloneDX SBOM from JFrog | Image Scans and SBOMs |
| `jfrogPromote` | Promote image from staging to prod repo | Promote to Verified Repo |

### Needs credentials or configuration to test

| Step | Description | Needs | In demo pipeline |
| ---- | ----------- | ----- | ---------------- |
| `appSidecarPod` | Run the built image as a sidecar in a test pod | `IMAGE_PULL_SECRET`: dockerconfigjson Secret for `itopsplatf-prod` in the agent namespace | Image Tests |
| `smokeTest` | HTTP health check with retries | Image pull secret (above); dev deploy for the dev run | Image Tests; Dev Verification (commented) |
| `playwrightTest` | Playwright browser tests | Image pull secret (above) | Image Tests; Dev Verification (commented) |
| `jmeterTest` | JMeter load/smoke plans | Image pull secret (above) | Image Tests; Dev Verification (commented) |
| `gitopsPromote` | Update image tags in the GitOps manifest repo | `github-org-token`; `overlays/dev` in the manifests repo | Update Image Tags |
| `sonarqubeScan` | SonarQube analysis | SonarQube token credential and project key | Source Scans (commented) |
| `snykCodeScan` | Snyk Code SAST scan | `snyk-api-token` | Source Scans (commented) |
| `snykDependencyScan` | Snyk Open Source SCA scan | `snyk-api-token` | Not yet wired |
| `snykContainerScan` | Snyk container scan + SBOM | `snyk-api-token` | Image Scans (commented) |
| `cosignSign` | Sign image with cosign + AWS KMS | `COSIGN_KMS_KEY_ARN`; a real cosign image pin in `resources/pods/cosign.yaml` | Runs, skips signing |
| `cosignAttest` | Attach SBOM attestation | Same as `cosignSign` | Runs, skips attestation |
| `cosignVerify` | Verify image signature | Same as `cosignSign` | Commented |
| `awsAssumeRole` | AWS STS AssumeRole | `aws-demo-credentials`, `AWS_DEMO_ACCOUNT_ID` (optionally `AWS_DEMO_ROLE_NAME`) | Terraform Plan (dev) |
| `tfenvDeploy` | Terraform init/plan/apply from the IronBank image | Same as `awsAssumeRole` | Terraform Plan (dev), plan only |
| `s3CacheSync` | Restore/save build caches in S3 | `S3_BUILD_CACHE_BUCKET` and AWS access | Build steps, skipped when unset |
| `slackNotify` | Slack webhook notification | `slack-webhook-url` | post, best effort |
| `jiraUpdate` | Comment on / transition a Jira issue | `jira-api-token`; issue key in `JIRA_ISSUE_KEY` or a commit message | post, best effort |
| `serviceNowUpdate` | Create or comment on a ServiceNow record | ServiceNow token credential | Not yet wired |
| `kustomizeDeploy` | kubectl apply of a kustomize overlay | `kubeconfig-<cluster>`, dev namespace | Deploy to Dev (commented) |
| `deployToK8s` | kubectl rollout to Kubernetes | `kubeconfig-<cluster>` | Not yet wired |
| `helmDeploy` | Helm upgrade/install | `kubeconfig-<cluster>` | Not yet wired |
| `fluxcdDeploy` | Flux reconcile | `kubeconfig-<cluster>` | Not yet wired |
| `argocdDeploy` | Argo CD sync | `argocd-auth-token` | Not yet wired |
| `rollbackK8s` | kubectl rollout undo | `kubeconfig-<cluster>` | Not yet wired |
| `seleniumTest` | Selenium tests on selenium.cloud.cms.gov | A dev deploy the Selenium box can reach | Not yet wired |

### Not available to test in the demo pipeline

| Step | Description | Why |
| ---- | ----------- | --- |
| `openscapScan` | OpenSCAP policy scan of an image | The pinned oscap image ships no SCAP Security Guide datastreams |
| `dockerBuild` | Podman build + push to registry | Superseded by `jfrogBuildPublish` here; still used by `templates/web-app` |
| `jfrogBuild` | Podman build + push to JFrog Artifactory | Superseded by `jfrogBuildPublish` here; still used by `templates/jfrog-secure` |
| `terraformDeploy` | Terraform via tfenv + Terraform Cloud token | Installs tools with `apk` in a non-root pod and downloads tfenv/terraform at run time; the demo uses `tfenvDeploy` |
| `cloudformationDeploy` | CloudFormation stack deploy | No CloudFormation demo (and needs AWS credentials) |
| `ansibleDeploy` | Ansible playbook run | No inventory or target hosts for the demos |
| `cypressTest` | Cypress browser tests | No Cypress suite; browser tests use Playwright |

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

## Development Setup

Contributing to this repository (as opposed to consuming the catalog) requires a
one-time setup per clone:

```
make init
```

This installs the `prek` hook shims for the `pre-commit`, `commit-msg` and
`pre-push` stages, and sets `commit.template` to `.gitmessage`. Both are local
git state and cannot be committed, so a fresh clone has neither until you run it.

Prerequisites: `prek`, `gitlint`, `yamllint`, `conftest` and `gitleaks` on
`PATH`.

### Commit Messages

Commit messages follow [ConventionalCommits](https://www.conventionalcommits.org/en/v1.0.0/)
and are enforced by `gitlint` at the `commit-msg` stage. `.gitlint` is
authoritative for the permitted types and line limits.

`.gitmessage` is the authoring scaffold that `make init` wires up — it prefills
the editor with the format as comments, which git strips. It is a reminder, not
the gate, so `git commit -m` bypasses it but still gets checked by the hook.

Available `make` targets:

| Target                   | Purpose                                                |
| ------------------------ | ------------------------------------------------------ |
| `make init`              | Per-clone setup: hooks and commit template             |
| `make lint`              | yamllint, policy checks, `template.yaml` schema        |
| `make lint-jenkinsfiles` | Validate all Jenkinsfiles against the controller (~3s) |
| `make lint-secrets`      | Full-history secret scan (~10s, outside all hooks)     |

The staging rationale for each check is documented in [process.md](process.md).

### Testing Against a Jenkins Controller

Two scripts talk to a live controller. Both need `JENKINS_URL`, `JENKINS_USER`
and `JENKINS_TOKEN`:

```
source .envrc.jenkins
```

**`scripts/jenkins-lint.sh`** validates declarative Jenkinsfiles against the
controller's `pipeline-model-converter/validate` endpoint. It also runs
automatically at pre-push on changed Jenkinsfiles. Without credentials or a
reachable controller it skips with a message and exits 0, so an offline push is
never blocked.

Three of the templates — `jfrog-secure`, `library-publish` and `multi-branch` —
are scripted pipelines with no `pipeline {}` block. The declarative linter cannot
validate those, so they are reported as `SKIP`. That is expected, not an
oversight.

**`scripts/jenkins-job.sh`** triggers builds and reads their output:

```
./scripts/jenkins-job.sh trigger demos BRANCH=main
./scripts/jenkins-job.sh status  demos
./scripts/jenkins-job.sh log     demos 42
./scripts/jenkins-job.sh tail    demos
```

It refuses job paths outside `JENKINS_JOB_PREFIX` (default `demos`) so a mistyped
argument cannot start another team's job. This is a guardrail, not a security
boundary — the token still reaches any job it has permission for via `curl`.

### MCP

A Jenkins [MCP server](https://github.com/jenkinsci/mcp-server-plugin) would let
an agent trigger builds and read logs directly, replacing `jenkins-job.sh`. It is
a **controller-side plugin**, so it must be installed on the controller by an
administrator; it is not currently installed. Note it exposes no Jenkinsfile
validation tool, so `jenkins-lint.sh` stays useful either way.

## Migration from JTE

| JTE Concept                         | PTC Equivalent                                   |
| ----------------------------------- | ------------------------------------------------ |
| `pipeline_config.groovy` (org)      | `catalog.yaml` (static metadata only)            |
| `pipeline_config.groovy` (team)     | Template parameters (set at job creation)        |
| `libraries { lib { param = val } }` | `vars/` shared library + template parameters     |
| `library_config.groovy` fields      | `template.yaml` parameters section               |
| `template_methods {}`               | Steps called directly in Jenkinsfile             |
| `on_pull_request` / `on_merge`      | Branch conditionals in Jenkinsfile               |
| JTE governance tiers                | CloudBees RBAC + template catalog access control |
