#!/usr/bin/env bash
# Validates declarative Jenkinsfiles against a live controller's
# pipeline-model-converter endpoint. This is the only check that catches a
# malformed `pipeline {}` block before a push, because declarative syntax is
# defined by the plugins installed on the controller rather than by any grammar
# a local parser could carry.
#
# Requires JENKINS_URL, JENKINS_USER and JENKINS_TOKEN — see .envrc.jenkins.
# Absent or unreachable, the check skips rather than fails: a network round trip
# must never be the reason a developer cannot push from a plane.
set -eu

usage() {
    echo "usage: $0 [FILE...]" >&2
    echo "  With no arguments, validates every Jenkinsfile under templates/ and demos/." >&2
}

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
    usage
    exit 0
fi

if [ -z "${JENKINS_URL:-}" ] || [ -z "${JENKINS_USER:-}" ] || [ -z "${JENKINS_TOKEN:-}" ]; then
    echo "jenkins-lint: JENKINS_URL/JENKINS_USER/JENKINS_TOKEN not set, skipping." >&2
    echo "  source .envrc.jenkins to enable Jenkinsfile validation." >&2
    exit 0
fi

# Trailing slash is load-bearing: every endpoint below is appended directly.
BASE="${JENKINS_URL%/}/"
CURL_AUTH="--user ${JENKINS_USER}:${JENKINS_TOKEN}"

if [ "$#" -gt 0 ]; then
    FILES="$*"
else
    FILES="$(find templates demos -name Jenkinsfile -type f | sort)"
fi

if [ -z "$FILES" ]; then
    echo "jenkins-lint: no Jenkinsfiles to validate."
    exit 0
fi

# One crumb is reused for every file. Fetching it also doubles as the
# reachability probe, so an unreachable controller costs one short timeout
# rather than one per file.
# shellcheck disable=SC2086 # CURL_AUTH must word-split into two curl args.
CRUMB_JSON="$(curl -sS --max-time 10 $CURL_AUTH "${BASE}crumbIssuer/api/json" 2>/dev/null || true)"
CRUMB="$(printf '%s' "$CRUMB_JSON" | sed -n 's/.*"crumb":"\([^"]*\)".*/\1/p')"

if [ -z "$CRUMB" ]; then
    echo "jenkins-lint: could not reach ${BASE} or authenticate, skipping." >&2
    exit 0
fi

FAILED=0
PASSED=0
SKIPPED=0

for f in $FILES; do
    if [ ! -f "$f" ]; then
        continue
    fi

    # Scripted pipelines have no `pipeline {}` block, and the endpoint rejects
    # them with "did not contain the 'pipeline' step". That is a structural
    # limit of the declarative linter, not a defect in the file, so they are
    # reported as skipped to keep the check's failures meaningful.
    if ! grep -q '^[[:space:]]*pipeline[[:space:]]*{' "$f"; then
        echo "SKIP  $f (scripted pipeline, no declarative block)"
        SKIPPED=$((SKIPPED + 1))
        continue
    fi

    # shellcheck disable=SC2086 # CURL_AUTH must word-split into two curl args.
    RESULT="$(curl -sS --max-time 30 $CURL_AUTH \
        -H "Jenkins-Crumb:${CRUMB}" \
        -F "jenkinsfile=<${f}" \
        "${BASE}pipeline-model-converter/validate" 2>/dev/null || true)"

    # The endpoint answers HTTP 200 for invalid input and reports the verdict in
    # the body, so the exit status has to come from the text. Treating a 200 as
    # success would make this check pass unconditionally.
    if printf '%s' "$RESULT" | grep -q 'successfully validated'; then
        echo "PASS  $f"
        PASSED=$((PASSED + 1))
    else
        echo "FAIL  $f"
        printf '%s\n' "$RESULT" | sed 's/^/        /'
        FAILED=$((FAILED + 1))
    fi
done

echo "jenkins-lint: ${PASSED} passed, ${FAILED} failed, ${SKIPPED} skipped."

if [ "$FAILED" -gt 0 ]; then
    exit 1
fi
