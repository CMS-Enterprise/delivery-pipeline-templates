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

These steps are available to all templates when this repo is configured as a Jenkins Shared Library:

| Step                 | Description                                |
| -------------------- | ------------------------------------------ |
| `buildGradle`        | Gradle build in ephemeral pod              |
| `buildNpm`           | npm ci + build in ephemeral pod            |
| `unitTestGradle`     | Gradle test execution                      |
| `unitTestNpm`        | npm test with coverage                     |
| `changedServices`    | Select which monorepo services a build needs |
| `dockerBuild`        | Podman build + push to registry            |
| `jfrogBuild`         | Podman build + push to JFrog Artifactory   |
| `jfrogBuildPublish`  | Podman build + push + sign, one image per call |
| `jfrogScan`          | JFrog Xray build scan                      |
| `jfrogPromote`       | Promote build from staging to prod repo    |
| `trivyScan`          | Trivy container vulnerability scan         |
| `snykCodeScan`       | Snyk Code SAST scan                        |
| `snykDependencyScan` | Snyk Open Source SCA scan                  |
| `deployToK8s`        | kubectl rollout to Kubernetes              |
| `rollbackK8s`        | kubectl rollout undo                       |
| `smokeTest`          | HTTP health check with retries             |
| `slackNotify`        | Slack webhook notification                 |
| `serviceNowUpdate`   | Create or comment on a ServiceNow record   |
| `cosignSign`         | Sign container image with cosign + AWS KMS |
| `cosignVerify`       | Verify container image signature           |
| `awsAssumeRole`      | AWS STS AssumeRole                         |

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
