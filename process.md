# Shift-Left Delivery Process

The principle behind this document is that the cost of a defect rises with the
distance between the moment it is introduced and the moment it is found. Each
stage below therefore runs the fastest checks that can still produce a
meaningful verdict, and defers slower checks to the next stage. The ordering is
deliberate: a developer or agent should receive feedback while the work is still
in short-term memory or context.

## Two audiences

This document describes two different things, and confusing them makes
implemented work look missing:

- **Sections 1-4** govern **this repository's own development**. They are
  implemented here, as local hooks driven by prek.
- **Sections 5-7** describe the **pipelines this repository produces for
  consuming projects**. They are implemented in `templates/` and `vars/`, not as
  checks that run against this repository. Repository-level CI is a separate,
  currently open item — see [Open items](#open-items).

## Stage placement is decided by measurement

A check belongs at the earliest stage whose time budget it fits. That is a
judgement about runtime, and it decays as tools and repositories grow, so the
measurements are recorded here rather than recalled. All figures are from this
tree.

| Check                       | Scope                        | Runtime |
| --------------------------- | ---------------------------- | ------- |
| `gitleaks --staged`         | staged files                 | 0.30s   |
| `yamllint`                  | 48 yaml files                | 0.13s   |
| `conftest verify`           | 4 rego files                 | 0.011s  |
| `conftest test`             | 38 pod manifests, 456 assertions | 0.021s |
| `gitleaks` range scan       | 5 commits                    | 0.034s  |
| `gitleaks` range scan       | 20 commits                   | 0.61s   |
| `gitleaks` full history     | 631 commits                  | 9.6s    |
| **full pre-commit stage**   | all files                    | 0.35s   |
| **full pre-push stage**     | all files, 5-commit range    | 0.57s   |
| **`make lint`**             | all files                    | 0.17s   |

Two consequences worth stating explicitly:

- `conftest` costs 0.03s, not the JVM-scale cost its reputation suggests. It is
  cheap enough for pre-commit, and that is where it runs.
- A full-history secret scan costs 9.6s and is therefore in no hook at all. It
  is available as `make lint-secrets`. The hooks scan a commit range instead.

## 1. Authoring Standards

Standards are encoded in configuration rather than documented as conventions, so
that they apply without being remembered.

- **`.editorconfig`** establishes indentation, line endings and final-newline
  behaviour for every editor and IDE that supports it.
- **`AGENTS.md`** carries the same standards to coding agents, which do not read
  editor configuration.
- **Format on save**, so that formatting never appears in a review diff.
- **Linters integrated into the IDE**, surfacing findings at the point of
  authorship rather than at the point of commit.

Recommended IDE linters:

| Linter            | Purpose                                   |
| ----------------- | ----------------------------------------- |
| SonarLint         | Code quality and maintainability          |
| Snyk              | Dependency and code vulnerabilities       |
| Language-specific | ESLint, Pylint, golangci-lint, Checkstyle |

Prefer widely adopted rule sets over personally preferred ones. A conventional
configuration is understood by new contributors, is supported by tooling
defaults, and does not require justification in review.

### Formatting scope

`dprint` is fast, but its plugin coverage is strongest for JavaScript,
TypeScript, JSON, Markdown, TOML and YAML. This repository is predominantly
Groovy, which dprint cannot format, so it is **not adopted** — introducing a
formatter that cannot touch the majority of the tree would imply a guarantee it
does not provide. `.editorconfig` covers Groovy indentation instead.

`npm-groovy-lint` is **not run at any stage**. Its finding volume on this tree is
too high to gate a commit on, and `--format` rewrites PTC `${placeholders}` into
invalid template syntax. Groovy correctness is established by review and by CI
executing the templates, not by a local linter.

## 2. Pre-Commit

Pre-commit exists to protect the commit history, and its overriding constraint
is speed. A hook that is slow enough to interrupt the developer's train of
thought will eventually be bypassed with `--no-verify`, at which point it
protects nothing. The whole stage costs 0.35s.

Currently enforced:

- **Secret and credential detection** (`gitleaks`, staged files). A committed
  secret must be rotated even if the commit is later amended, so this check has
  to run before the commit object exists.
- **Formatting and whitespace fixers**, so that no commit mixes formatting churn
  with behavioural change.
- **Well-formedness** of JSON and YAML, plus merge-conflict and private-key
  detection.
- **`yamllint`** on changed files under `resources/` and `templates/`.
- **`conftest test`** against the pod manifests, when a manifest or a policy
  changes.
- **Shell linting** of changed shell scripts.

Tooling notes:

- **prek** is a drop-in reimplementation of the `pre-commit` framework, chosen
  for lower startup overhead while remaining compatible with existing
  `.pre-commit-config.yaml` files. It drives all three hook stages here.
- `jenkinsfilelint` is deliberately absent: it validates by POSTing to a running
  Jenkins controller, which is not reachable from a workstation.

### Bypass policy

`--no-verify` is legitimate in narrow cases: committing known-broken work in
progress to a private branch, or an incident fix where the hook itself is the
obstacle. It is not legitimate as a way to avoid fixing a finding.

The rule is that **a bypassed commit must not reach a shared branch
unexamined**, and the compensating control is the pre-push secret scan in
section 4. That scan covers the whole range being pushed rather than the staged
set, so a secret that skipped pre-commit is still caught before it leaves the
workstation.

This is a real limitation, not a solved problem: a check enforced only locally
is not enforced at all for anyone who does not run it. Server-side enforcement
is required to close that gap, and is part of the open CI item.

## 3. Commit Message Conventions

Enforced by `gitlint` at the `commit-msg` stage, configured in `.gitlint` to
require the ConventionalCommits format with an explicit type list.

A conventional commit format is worth enforcing for three distinct reasons,
which are often collapsed into one:

1. **Auditability.** A consistent format makes history searchable, which matters
   when reconstructing when a change entered a release.
2. **Signal-to-noise.** Conventions reduce the volume of message text that
   carries no information.
3. **Pipeline control.** Tags, message trailers and branch names can direct
   downstream CI behaviour, allowing one pipeline definition to serve several
   change classes. This routing is only safe once the format is actually
   enforced, which is the main reason the hook exists.

The `body-is-missing` rule is disabled, because requiring a body on a one-line
change produces filler text rather than information. History predating this hook
does not conform and has not been rewritten.

## 4. Pre-Push

Pre-push occupies the space between pre-commit and CI. The budget is larger than
pre-commit's, because the developer is pausing to publish rather than mid-edit,
but smaller than CI's. The whole stage costs 0.57s.

The distinguishing property of this stage is **scope**, not just budget. A
pre-commit hook sees only the files in one commit, which is blind to two
failure modes:

- A policy change that breaks a manifest nobody touched in that commit. So
  `yamllint`, `conftest verify` and `conftest test` all re-run across the **full
  tree** here.
- A secret in a commit that reached history via `--no-verify`. So `gitleaks`
  re-runs across the **whole range being pushed**, via
  `scripts/gitleaks-range.sh`.

The range script resolves `PRE_COMMIT_FROM_REF..PRE_COMMIT_TO_REF`, falls back
to the default branch when the remote branch does not exist yet, and skips with
a message when `gitleaks` is not installed so that a fresh clone can still push.

### Local tool prerequisites

`conftest` and `gitleaks` are Go binaries on the developer's workstation:

```
go install github.com/open-policy-agent/conftest@v0.69.0
go install github.com/zricethezav/gitleaks/v8@v8.30.0
```

The `gitleaks` version matches the `rev` pinned in `.pre-commit-config.yaml`, so
both stages apply the same detection rules. See
[Supply chain](#supply-chain-constraints) for why these are an exception to the
container-image rule.

## 5. Continuous Integration

_Describes generated pipelines. See [Open items](#open-items) for this
repository's own CI._

CI runs the comprehensive scans and tests that establish the behaviour of a
change, and is the authority on whether an artifact may be promoted.

- Build development artifacts, scan them, and promote only on a pass. Nothing
  reaches the shared registry ahead of its scan results.
- Record build metadata into the JFrog build info — `build-collect-env` and
  `build-add-git` — so that an Xray finding can be traced back to the commit
  that introduced it. Implemented in `vars/jfrogBuildPublish.groovy`.
- Use branch names and flags to select behaviour. A hotfix branch, for example,
  may reasonably skip the scans that have already cleared the main line, on the
  grounds that it is a narrow change made under incident time pressure and the
  surrounding content is already known-good.

## 6. Continuous Delivery

_Describes generated pipelines._

CD is triggered by the promotion of a production artifact and is responsible for
verifying it in a running environment rather than in isolation.

1. Deploy automatically into the implementation environment.
2. Execute the test suite against the running environment.
3. On success, perform a rolling or blue/green deployment.

Every deployment requires a timeout and a rollback path, so that a failure is
both detected and corrected without manual intervention.

## 7. Feature Flags

Feature flags decouple deployment from release. Code reaches production disabled
and is enabled independently, which reduces the blast radius of a change and
removes the need to redeploy in order to withdraw a feature.

## Supply chain constraints

These constraints are recorded authoritatively in `AGENTS.md` and shape every
stage above.

- All tooling in a pipeline arrives as a **pinned container image** from
  `artifactory.cloud.cms.gov`, referenced by digest.
- **Nothing is piped from `curl` into a shell.**
- Any artifact originating **outside JFrog is scanned with ClamAV** before use,
  with `freshclam` pointed at the artifactory mirror.
- Image digests are maintained by `scripts/update-pins.sh` and Renovate.

**The local hooks are a scoped exception.** `conftest` and `gitleaks` are
installed with `go install` from upstream module sources, because
`artifactory.cloud.cms.gov` does not resolve from a developer workstation
outside the cluster network. This is acceptable only because these binaries
**gate local commits and never touch a build artifact** — nothing they produce
is published, signed or promoted. Pipeline tooling has no such exemption. If the
artifactory mirror becomes reachable from workstations, these should move to
pinned images like everything else.

## Open items

- **This repository has no CI of its own.** There is no root `Jenkinsfile`, so
  `make lint` and the policy tests run only locally, and the locally-enforced
  checks above have no server-side equivalent. Jenkins is the intended platform.
- **`conftest verify` has nothing to verify.** It reports `0 tests` because
  `policy/` contains no `*_test.rego` files. The hook is wired up and will start
  producing a verdict as soon as policy unit tests are written; today only
  `conftest test` is doing work.
- **Groovy has no automated correctness check.** With `npm-groovy-lint`
  excluded, nothing mechanically validates the templates in `templates/` — the
  gap between a syntax error and a consumer's broken build is currently review
  alone. This is the strongest argument for the CI item above.
