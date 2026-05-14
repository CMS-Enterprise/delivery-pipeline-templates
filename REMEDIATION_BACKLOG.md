# CMS TRA Remediation Backlog — Delivery Pipeline Templates

**Audit Date:** 2026-05-04  
**Audited Scope:** `templates/sast/`, `templates/delivery/`, `templates/deployment/`, `templates/jenkins-secret-provisioner/`  
**TRA Compliance Result:** Non-compliant across all four contexts (CI/CD DevSecOps Pipeline, Container Deployment, Identity Management, Cloud Infrastructure)  
**Backlog Owner:** Platform Engineering / DevSecOps  

---

## Definition of Done (Secure Pipeline Changes)

All items in this backlog are "Done" when:

- [ ] Code change merged to `main` with peer review
- [ ] Relevant TRA rule ID cited in the PR description
- [ ] No new `latest` or undigested image tags introduced
- [ ] `kubectl auth can-i` or equivalent RBAC check passes with least-privilege
- [ ] Pipeline run succeeds end-to-end in a non-prod environment
- [ ] Security scan (SAST + container scan) returns no new HIGH/CRITICAL findings
- [ ] Change documented in `CHANGELOG.md`

---

## Epic 1 — Quick Wins: Eliminate Critical Vulnerabilities (0–30 days)

**Goal:** Remove immediate high-risk violations of mandatory TRA Business Rules.

---

### Story 1.1 — Pin All Container Image Tags to Immutable Digests

**TRA Rules:** BR-OR-1, BR-SBI-2, RP-CA-1  
**Priority:** Critical  
**Effort:** S (2–4 hrs)

**Problem:** All four Jenkinsfiles pull mutable image tags (`docker:latest`, `clamav/clamav:latest`, `snyk/snyk:alpine`, `owasp/dependency-check-action:latest`). A supply-chain compromise or tag overwrite silently changes pipeline behavior.

**Affected Files & Lines:**

| File | Line | Current Value | Required Fix |
|------|------|---------------|--------------|
| `templates/delivery/Jenkinsfile` | 50 | `docker:latest` | `docker:27.5.1@sha256:<digest>` |
| `templates/delivery/Jenkinsfile` | 57 | `clamav/clamav:latest` | `clamav/clamav:1.4.2@sha256:<digest>` |
| `templates/delivery/Jenkinsfile` | 66 | `snyk/snyk:alpine` | `snyk/snyk:alpine@sha256:<digest>` |
| `templates/sast/Jenkinsfile` | 8 | `owasp/dependency-check-action:latest` | pin to specific release digest |
| `templates/sast/Jenkinsfile` | (sonar scanner) | mutable tag | pin to digest |

**Acceptance Criteria:**

- [ ] Every container image reference in all Jenkinsfiles uses `image:tag@sha256:<digest>` format
- [ ] A `PINNED_IMAGES.md` or inline comment records the resolved digest date for each image
- [ ] `imagePullPolicy: IfNotPresent` set where digest is pinned (digest already guarantees immutability)
- [ ] CI pipeline validates no `latest` or digest-less references via a `grep` lint step
- [ ] Images sourced from Artifactory (`artifactory.cloud.cms.gov`) where CMS-approved mirrors exist, satisfying BR-OR-5

---

### Story 1.2 — Remove `StrictHostKeyChecking=no` from Deployment Pipeline

**TRA Rules:** BR-URL-5, BR-OR-3, RP-CA-6  
**Priority:** Critical  
**Effort:** S (1–2 hrs)

**Problem:** `templates/deployment/Jenkinsfile` lines 94 and 120 use `ssh -o StrictHostKeyChecking=no`, disabling host key verification and enabling MITM attacks against the GitOps push step.

**Affected Files & Lines:**

| File | Lines | Current Value |
|------|-------|---------------|
| `templates/deployment/Jenkinsfile` | 94, 120 | `ssh -o StrictHostKeyChecking=no` |

**Acceptance Criteria:**

- [ ] `StrictHostKeyChecking=no` removed from both occurrences
- [ ] Known-hosts file pre-seeded in the Jenkins agent pod spec or mounted as a ConfigMap (`ssh-keyscan` output committed to repo)
- [ ] Alternatively, switch GitOps push to HTTPS with token (no SSH) and remove SSH entirely
- [ ] Deployment pipeline run validates the host key during the git push step
- [ ] No `StrictHostKeyChecking=no` or `StrictHostKeyChecking=accept-new` appears in any template (enforced by lint)

---

### Story 1.3 — Remove Credentials from HTTPS Git Push URL

**TRA Rules:** BR-URL-5, BR-KSM-4  
**Priority:** Critical  
**Effort:** S (2–3 hrs)

**Problem:** `templates/deployment/Jenkinsfile` line 88 constructs an HTTPS push URL that embeds the `GIT_CREDENTIALS` value directly in the URL string. This exposes secrets in Jenkins logs, process listings, and git reflog.

**Affected Files & Lines:**

| File | Line | Current Pattern |
|------|------|-----------------|
| `templates/deployment/Jenkinsfile` | 88 | `https://${GIT_CREDENTIALS}@<host>/...` |

**Acceptance Criteria:**

- [ ] Credential embedded URL replaced with `git credential.helper` or `git push` via `GIT_ASKPASS` env var
- [ ] OR switch transport to SSH with a key credential (not password-in-URL)
- [ ] Jenkins log masking confirmed — no token appears in archived build logs
- [ ] `withCredentials` wrapper used so Jenkins masks the value automatically
- [ ] Credential never interpolated directly into a URL string

---

### Story 1.4 — Enforce `continue_on_image_scan_failure=false` as Non-Overridable Default

**TRA Rules:** BR-OR-1, BR-OR-3, RP-CA-9  
**Priority:** High  
**Effort:** S (1–2 hrs)

**Problem:** `templates/delivery/template.yaml` line 86 exposes `continue_on_image_scan_failure` as a configurable parameter with default `false`. Teams can set `true` at catalog instantiation, bypassing the container vulnerability gate entirely.

**Affected Files & Lines:**

| File | Line | Current |
|------|------|---------|
| `templates/delivery/template.yaml` | 86 | `default: false` (overridable) |
| `templates/delivery/Jenkinsfile` | (scan step) | respects override |

**Acceptance Criteria:**

- [ ] `continue_on_image_scan_failure` parameter removed from `template.yaml` (no longer a caller-configurable option)
- [ ] Jenkinsfile hardcodes `failOnIssues: true` (or equivalent) for the container scan step
- [ ] Exception path: if a team has a documented risk acceptance, the override must require a separate approval parameter gated by a shared-library policy function, not a simple boolean
- [ ] README updated to document that the scan gate is mandatory per CMS TRA BR-OR-1

---

### Story 1.5 — Add Pod Security Contexts to All Jenkins Agent Pod Specs

**TRA Rules:** BR-OR-1, RP-CA-3, RP-CA-4, RP-CA-8  
**Priority:** High  
**Effort:** M (4–6 hrs)

**Problem:** Three of four Jenkinsfiles (`sast`, `delivery`, `deployment`) define pod templates with no `securityContext`. The `jenkins-secret-provisioner` template has `runAsUser: 1001` on one container but no pod-level context and no `readOnlyRootFilesystem` or capability drops.

**Affected Files:**

- `templates/sast/Jenkinsfile`
- `templates/delivery/Jenkinsfile`
- `templates/deployment/Jenkinsfile`
- `templates/jenkins-secret-provisioner/Jenkinsfile`

**Acceptance Criteria:**

- [ ] All pod specs include a pod-level `securityContext`:
  ```yaml
  securityContext:
    runAsNonRoot: true
    runAsUser: 1000        # or image-specific non-root UID
    fsGroup: 1000
    seccompProfile:
      type: RuntimeDefault
  ```
- [ ] All containers include a container-level `securityContext`:
  ```yaml
  securityContext:
    allowPrivilegeEscalation: false
    readOnlyRootFilesystem: true
    capabilities:
      drop: ["ALL"]
  ```
- [ ] `readOnlyRootFilesystem: true` paired with `emptyDir` volume mounts for any writable paths the tool requires (e.g., `/tmp`, `/home/scanner/.sonar`)
- [ ] Docker-in-Docker (`docker:latest`) container reviewed separately — document why privileged is required if it remains, or replace with rootless BuildKit
- [ ] All pods pass `kubectl auth can-i` checks with least-privilege service account

---

## Epic 2 — Control Strengthening (30–60 days)

**Goal:** Harden pipeline controls, migrate to KSM-compliant secret management, and establish security visibility.

---

### Story 2.1 — Replace Runtime Kustomize Binary Download with Pinned In-Image Binary

**TRA Rules:** BR-SBI-2, BR-OR-1, RP-SBI-4  
**Priority:** High  
**Effort:** M (4–8 hrs)

**Problem:** `templates/deployment/Jenkinsfile` downloads the `kustomize` binary from `github.com` at runtime. This creates an untrusted runtime dependency: the download URL could be hijacked, the binary could change between runs, or network failures break deployments.

**Acceptance Criteria:**

- [ ] `kustomize` binary baked into a versioned, digested custom agent image hosted in Artifactory (`artifactory.cloud.cms.gov`)
- [ ] Binary version pinned and verified via SHA-256 checksum (even if bundled in image, document version)
- [ ] No `curl | sh` or dynamic binary download patterns in any Jenkinsfile
- [ ] Lint check added to CI to reject any `curl.*github` or `wget.*releases` patterns in templates

---

### Story 2.2 — Migrate Secret Provisioner from Static Replication to KSM-Native Fetch

**TRA Rules:** BR-KSM-1, BR-KSM-2, BR-KSM-4, RP-KSM-1, RP-KSM-2  
**Priority:** High  
**Effort:** L (1–2 weeks)

**Problem:** `templates/jenkins-secret-provisioner/Jenkinsfile` replicates secrets from Jenkins credentials into static Kubernetes `Secret` objects. This model:
- Creates a persistent secret copy that may drift from the source of truth
- Does not leverage CMS KSM (Key and Secret Management) lifecycle controls (rotation, audit, RBAC)
- Violates BR-KSM-1 (KSM auditing), BR-KSM-2 (KSM RBAC), BR-KSM-4 (credential leak audit)

**Acceptance Criteria:**

- [ ] Secrets fetched at pod startup via KSM integration (e.g., AWS Secrets Manager CSI driver, Vault Agent Injector, or CMS-approved equivalent)
- [ ] Static `kubectl create secret` calls removed from the Jenkinsfile
- [ ] All secret access events logged to the CMS security monitoring platform (satisfies BR-KSM-1)
- [ ] RBAC: service account used by the provisioner has `get`/`list` on secrets only in its own namespace (satisfies BR-KSM-2)
- [ ] Secret rotation tested: rotating a secret in KSM propagates to pods without a pipeline re-run
- [ ] A migration runbook created for teams using the old static provisioner

---

### Story 2.3 — Add Policy Gate Before Image Publish (Delivery Pipeline)

**TRA Rules:** BR-OR-1, BR-OR-5, RP-CA-9, RP-CA-10  
**Priority:** Medium  
**Effort:** M (4–6 hrs)

**Problem:** The delivery pipeline publishes an image to Artifactory only after Snyk and ClamAV scans, but there is no policy gate that evaluates the combined scan results and blocks publish on any HIGH/CRITICAL finding. The `continue_on_image_scan_failure` bypass (addressed in Story 1.4) is one expression of this gap.

**Acceptance Criteria:**

- [ ] A shared-library `policyGate()` function evaluates:
  - Snyk container scan: 0 CRITICAL, ≤ threshold HIGH (configurable, default 0)
  - ClamAV: 0 infections
  - SBOM generated and attached to build artifact
- [ ] `policyGate()` called as the final step before `skopeo copy` (publish)
- [ ] Gate result logged with build URL and image digest to a centralized audit log
- [ ] Bypass requires a signed exception in the catalog parameter — not a simple boolean

---

### Story 2.4 — Centralize Security Event Logging from All Pipeline Steps

**TRA Rules:** BR-OR-2, BR-OR-3, RP-CA-11  
**Priority:** Medium  
**Effort:** L (1–2 weeks)

**Problem:** No Jenkinsfile forwards scan results, policy decisions, or secret-access events to a centralized security monitoring platform. BR-OR-2 requires containers to have security monitoring; BR-OR-3 requires deployment infrastructure to be hardened and monitored.

**Acceptance Criteria:**

- [ ] All security scan results (SonarQube, OWASP DC, Snyk, ClamAV) shipped to the CMS security monitoring platform (SIEM/Splunk/CloudWatch) as structured JSON events
- [ ] A shared-library `securityEventEmit()` wrapper standardizes the event schema: `{timestamp, pipeline, stage, tool, result, imageDigest, buildUrl}`
- [ ] Secret provisioner logs every credential access event to the same SIEM
- [ ] Deployment pipeline logs every `kubectl apply` / `kustomize build` invocation with actor identity
- [ ] Runbook created for the SOC: "How to query pipeline security events"

---

## Epic 3 — Advanced Maturity (60–90 days)

**Goal:** Achieve artifact signing, provenance attestation, and admission-time enforcement.

---

### Story 3.1 — Sign Published Images and Generate SLSA Provenance Attestations

**TRA Rules:** BR-OR-1, BR-SBI-2, RP-CA-10, RP-CA-11  
**Priority:** Medium  
**Effort:** L (1–2 weeks)

**Problem:** Images are published to Artifactory without a cryptographic signature or provenance attestation. A compromised Artifactory account or registry MITM attack can substitute images without detection.

**Acceptance Criteria:**

- [ ] `cosign sign` called after every successful `skopeo copy` using a KSM-managed signing key
- [ ] SLSA Level 2+ provenance attestation generated via `cosign attest` or `slsa-github-generator` equivalent
- [ ] Attestation attached to the image manifest in Artifactory
- [ ] Downstream deployment pipeline verifies signature before `kustomize build` or `kubectl apply`
- [ ] Signature verification failure blocks deployment (not a warning)
- [ ] Key rotation process documented and tested

---

### Story 3.2 — Deploy Admission Controller to Enforce Image Policy at Runtime

**TRA Rules:** BR-OR-1, BR-OR-3, RP-CA-1, RP-CA-9  
**Priority:** Medium  
**Effort:** XL (3–4 weeks, requires cluster-level access)

**Problem:** Even with pipeline-level controls, nothing prevents a developer with `kubectl` access from deploying an unsigned, unscanned, or `latest`-tagged image directly to the cluster.

**Acceptance Criteria:**

- [ ] Admission webhook (e.g., Kyverno or Gatekeeper) deployed to the CMS cluster
- [ ] Policy: reject any pod with `imagePullPolicy: Always` + mutable tag (no digest)
- [ ] Policy: reject any pod whose image lacks a valid cosign signature from the CMS signing key
- [ ] Policy: enforce pod security context requirements from Story 1.5 at admission time
- [ ] Policy violation events forwarded to SIEM (Story 2.4)
- [ ] Dry-run (audit) mode enabled first; enforcement mode gated on 30-day clean audit period
- [ ] All existing pipeline-deployed workloads validated against policies before enforcement mode

---

### Story 3.3 — Establish Quarterly Pipeline Security Maturity Scorecard

**TRA Rules:** BR-OR-2, BR-OR-3, BR-CCIC-01  
**Priority:** Low  
**Effort:** M (ongoing)

**Problem:** There is no mechanism to track regression or improvement in pipeline security posture over time. BR-CCIC-01 requires ATO-level documentation of security controls; the maturity scorecard is the operational artifact that feeds ATO evidence packages.

**Acceptance Criteria:**

- [ ] Scorecard template created in `docs/security/maturity-scorecard.md` covering:
  - Image hygiene (% pipelines with pinned digests)
  - Scan gate enforcement (% with non-bypassable gates)
  - Secret management compliance (% using KSM vs static secrets)
  - Artifact signing coverage (% images signed)
  - Pod security context coverage (% pods with required contexts)
- [ ] Scorecard populated from automated checks (CI job) and reviewed quarterly
- [ ] Findings fed into the ATO evidence package for the platform
- [ ] Regression from a previous quarter triggers a P2 remediation ticket within 5 business days

---

## Backlog Summary

| Story | Epic | TRA Rules | Priority | Effort | Target |
|-------|------|-----------|----------|--------|--------|
| 1.1 Pin image digests | 1 | BR-OR-1, BR-SBI-2, RP-CA-1 | Critical | S | 0–30 days |
| 1.2 Remove StrictHostKeyChecking=no | 1 | BR-URL-5, BR-OR-3, RP-CA-6 | Critical | S | 0–30 days |
| 1.3 Remove credentials from git URL | 1 | BR-URL-5, BR-KSM-4 | Critical | S | 0–30 days |
| 1.4 Harden scan-bypass parameter | 1 | BR-OR-1, BR-OR-3, RP-CA-9 | High | S | 0–30 days |
| 1.5 Add pod security contexts | 1 | BR-OR-1, RP-CA-3/4/8 | High | M | 0–30 days |
| 2.1 Pin kustomize binary in image | 2 | BR-SBI-2, BR-OR-1, RP-SBI-4 | High | M | 30–60 days |
| 2.2 Migrate to KSM secret fetch | 2 | BR-KSM-1/2/4, RP-KSM-1/2 | High | L | 30–60 days |
| 2.3 Add publish policy gate | 2 | BR-OR-1, BR-OR-5, RP-CA-9/10 | Medium | M | 30–60 days |
| 2.4 Centralize security logging | 2 | BR-OR-2, BR-OR-3, RP-CA-11 | Medium | L | 30–60 days |
| 3.1 Image signing + SLSA provenance | 3 | BR-OR-1, BR-SBI-2, RP-CA-10/11 | Medium | L | 60–90 days |
| 3.2 Admission controller enforcement | 3 | BR-OR-1, BR-OR-3, RP-CA-1/9 | Medium | XL | 60–90 days |
| 3.3 Quarterly maturity scorecard | 3 | BR-OR-2, BR-CCIC-01 | Low | M | Ongoing |

---

## References

- [CMS Technical Reference Architecture](https://cms-cloud-service-documentation.atlassian.net/wiki/spaces/CMSTRD/overview)
- [CMS TRA MCP Server](../cms-tra-mcp-main/)
- `templates/delivery/Jenkinsfile`
- `templates/sast/Jenkinsfile`
- `templates/deployment/Jenkinsfile`
- `templates/jenkins-secret-provisioner/Jenkinsfile`
- OWASP Supply Chain Security Top 10
- SLSA Framework — https://slsa.dev
