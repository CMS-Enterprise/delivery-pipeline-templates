# DAST Pipeline

The DAST Pipeline template runs Dynamic Application Security Testing (DAST) against a deployed application endpoint using OWASP ZAP.

## Parameters

The DAST Pipeline is a [parameterized pipeline](https://www.jenkins.io/doc/book/pipeline/syntax/#parameters) that is intended to be invoked from another pipeline. Some of the parameters can be given default values when an instance of the template is created.

- `target_url` (Pipeline + Template): The HTTP(S) URL to scan. Default: none.
- `zap_image` (Pipeline + Template): The scanner image used to run OWASP ZAP. Default: `artifactory.cloud.cms.gov/docker/zaproxy/zaproxy:stable`.
- `scan_type` (Pipeline + Template): Type of ZAP scan to run (`baseline` or `full`). Default: `baseline`.
- `spider_minutes` (Pipeline + Template): Maximum minutes for ZAP spidering during the scan. Default: `3`.
- `fail_on_severity` (Pipeline + Template): Minimum severity that will fail the pipeline when findings are present (`low`, `medium`, `high`, `critical`). Default: `high`.
- `max_allowed_high` (Pipeline + Template): Maximum high-severity findings allowed before failing the build. Default: `0`.
- `max_allowed_medium` (Pipeline + Template): Maximum medium-severity findings allowed before failing the build. Default: `0`.
- `report_base_name` (Pipeline + Template): Base filename to use for generated DAST artifacts. Default: `zap-dast-report`.
- `zap_additional_arguments` (Pipeline + Template): A JSON serialized array of additional arguments to pass to the ZAP script. Default: `[]`.
- `build_retention_days` (Template only): Number of days to retain build logs and artifacts. Default: `90`.
- `build_retention_count` (Template only): Number of builds to retain. Default: `1000`.

## Usage

For general usage see the [CMS ADO Pipeline Catalog README](../../README.md).

The DAST Pipeline is intended to be invoked dynamically from another Pipeline using the [Pipeline: Build Step plugin](https://plugins.jenkins.io/pipeline-build-step/). This allows a project-specific Jenkins pipeline to deploy or expose an application in a test environment, then invoke this template to run DAST checks before promoting the release.

```groovy
pipeline {
  stages {
    stage('DAST') {
      steps {
        build(job: 'App DAST', wait: true, propagate: true, parameters: [
          string(name: 'target_url', value: 'https://my-app-dev.example.com'),
          string(name: 'scan_type', value: 'baseline'),
          string(name: 'fail_on_severity', value: 'high'),
          string(name: 'max_allowed_high', value: '0'),
          string(name: 'max_allowed_medium', value: '0')
        ])
      }
    }
  }
}
```

## Security & Compliance

This DAST Pipeline template implements the following CMS TRA (Technical Reference Architecture) controls:

- **BR-SBI-2** — Container images are pinned to immutable digests (`image:tag@sha256:<digest>`) to prevent supply-chain compromise via tag rewrite.
- **BR-OR-1** — Security gate is non-bypassable. High and medium severity findings exceeding configured thresholds always fail the pipeline. No `continue_on_scan_failure` bypass.
- **RP-CA-3, RP-CA-4, RP-CA-8** — Pod and container security contexts enforce:
  - `runAsNonRoot: true` with non-root UID (1000)
  - `readOnlyRootFilesystem: false` (ZAP requires writable `/tmp` and home directory)
  - `allowPrivilegeEscalation: false`
  - Capability drops: `drop: [ALL]`
  - `seccompProfile: RuntimeDefault`

## DAST Artifacts

The DAST Pipeline archives the following artifacts:

- `<report_base_name>.json` — Machine-readable ZAP findings (can be ingested into SIEMs or compliance systems).
- `<report_base_name>.html` — Human-readable ZAP report with detailed vulnerability descriptions and remediation guidance.
- `<report_base_name>.md` — Markdown summary of findings.

## Updating the ZAP Scanner Image

The default ZAP image is pinned to a specific digest for supply-chain security. To update to a new ZAP version:

1. Pull the desired ZAP image version from Artifactory or Docker Hub.
2. Resolve its SHA-256 digest:

```bash
docker inspect artifactory.cloud.cms.gov/docker/zaproxy/zaproxy:stable | grep -i '"id"'
```

Then proceed with:

1. Update the `default_zap_image` parameter in this template's `template.yaml` to include the new digest.
2. Test the updated template in a non-prod environment before promoting.
3. Commit the change to this repository with a clear message citing the ZAP version update.

## Exception and Risk Acceptance

By design, the DAST gate cannot be bypassed via pipeline parameters. If a finding must be accepted despite the gate threshold, follow the CMS risk acceptance process:

1. Document the finding and justification in a risk register.
2. Obtain security sign-off from your Chief Information Security Officer (CISO) or delegated reviewer.
3. Once approved, create a separate Jenkins credential storing the exception flag.
4. Work with the Platform Engineering team to integrate the exception via a shared library `policyGate()` function (planned for REMEDIATION_BACKLOG Epic 2.3).
