# Agent Instructions

Standards for coding agents working in this repository. `.editorconfig` and the
linter configs carry the same rules to editors; this file carries them to agents,
which do not read editor configuration.

Statements are marked:

- **MUST** — an inviolable constraint. Do not vary it, and do not propose a
  change that works around it. Most exist for supply-chain or compliance
  reasons, and violating one invalidates the artifact rather than merely
  degrading it.
- **DEFAULT** — the established choice. Follow it unless the task gives a reason
  not to; a deviation needs to be stated, not hidden.

See `process.md` for how these constraints map onto the pre-commit, pre-push and
CI stages.

## Supply chain

- **MUST** obtain all pipeline tooling as a **pinned container image** from
  `artifactory.cloud.cms.gov`, referenced by digest rather than tag.
- **MUST NOT** pipe `curl` into a shell. No `curl ... | bash`, no `wget ... | sh`.
- **MUST** scan any artifact or container originating outside JFrog with ClamAV
  before use. `freshclam` points at `artifactory.cloud.cms.gov` for its cache.
- **MUST** scan the `tfenv` and `terraform` binaries before use, as with any
  other externally sourced binary.
- **MUST** give every download a static hash, updated by `scripts/update-pins.sh`
  and by Renovate.
- **MUST** keep a rotate script for every credential, connecting to the service
  to rotate it and storing the new value as a Jenkins credential.
- **DEFAULT**: the squid proxy that allows only `artifactory.cloud.cms.gov`
  belongs to the **ironbank / batcave** builds, not the standard pipeline. See
  `templates/ironbankish/` and `ironbankbuild.md`. The standard pod-pipeline does
  not proxy its build egress.

## Build and signing

- **MUST** sign containers with cosign, and **MUST** verify the cosign signature
  before consuming a container.
- **DEFAULT**: build containers with podman, using the reproducible-builds flag.
- **DEFAULT**: record build metadata into the JFrog build info via
  `build-collect-env` and `build-add-git`, so an Xray finding traces back to the
  commit that introduced it. See `vars/jfrogBuildPublish.groovy`.

## Scanning

- **MUST** pass scans before anything reaches JFrog. Nothing is uploaded ahead of
  its scan results; upload does not overlap with scanning.
- **MUST** use **trivy** as the vulnerability scanner, not grype.
- **MUST** lint all code.
- **DEFAULT**: scan all code with trivy, sonarqube and snyk, and run those scans
  **in parallel**.
- **DEFAULT**: scan containers with JFrog Xray and snyk.
- **DEFAULT**: scan containers for policy violations with OpenSCAP.
- **DEFAULT**: create SBOMs with JFrog Xray and with snyk; scan the SBOM and keep
  it as a build artifact.
- **DEFAULT**: a container may additionally be scanned with zap.

## Test

- **DEFAULT**: run unit tests appropriate to the language and test type, e.g.
  junit, cucumber.
- **DEFAULT**: run selenium tests via the selenium box installation on
  `selenium.cloud.cms.gov`; likewise playwright.
- **DEFAULT**: run jmeter tests.

## Provisioning

- **MUST** run `vars/awsAssumeRole.groovy` before anything that touches AWS —
  including terraform, cloudformation, and ansible when used with AWS SSM — in
  order to obtain the correct credentials.
- **DEFAULT**: use terraform to stand up and tear down an environment, with
  `tfenv` allowing the code to specify the version.
- **DEFAULT**: use cloudformation to stand up and tear down an environment,
  including a stack to stand up DLTA and one to tear it down.

## Deployment

- **MUST** give every deployment a timeout and a rollback method, so a failure is
  both detected and corrected without manual intervention.
- **MUST** follow gitops methodology for kubectl, fluxcd and argocd updates.
- **DEFAULT**: update a service with kubectl, fluxcd, argocd or ansible.

## Repository conventions

- **MUST NOT** run `npm-groovy-lint --format`. It rewrites PTC `${placeholders}`
  into invalid template syntax. The linter is not part of any hook stage; see
  `process.md`.
- **MUST** use conventional commit messages — enforced by `gitlint` at the
  `commit-msg` stage, configured in `.gitlint`.
- **DEFAULT**: 4-space indentation for Groovy and Jenkinsfiles, 2-space for YAML
  and JSON, per `.editorconfig`.
- **DEFAULT**: `demos/` exists to demonstrate the templates. Treat it as sample
  content, not library code.
