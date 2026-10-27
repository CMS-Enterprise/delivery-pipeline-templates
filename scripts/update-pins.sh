#!/usr/bin/env bash
set -uo pipefail

APPLY=false
if [[ "${1:-}" == "--apply" ]]; then APPLY=true; fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIBRARIES_DIR="$SCRIPT_DIR/../resources/pods/"

OUTDATED=0
UP_TO_DATE=0
SKIPPED=0
ERRORS=0

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

# --- Helper functions ---

# All images are pulled through Artifactory mirrors, so resolve digests
# from Artifactory — not from upstream registries. This ensures the digest
# we record actually exists and is pullable from where the pods fetch.
ARTIFACTORY="${ARTIFACTORY_REGISTRY:-artifactory.cloud.cms.gov}"

get_artifactory_digest() {
  local repo="$1"
  local image="$2"
  local tag="$3"
  local token
  token=$(curl -sf --max-time 15 \
    "https://${ARTIFACTORY}/v2/token?service=${ARTIFACTORY}&scope=repository:${repo}/${image}:pull" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null || true)
  if [[ -z "$token" ]]; then return; fi
  curl -sf --max-time 15 \
    -H "Authorization: Bearer ${token}" \
    -H "Accept: application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.v2+json" \
    "https://${ARTIFACTORY}/v2/${repo}/${image}/manifests/${tag}" \
    -D /dev/stderr -o /dev/null 2>&1 | grep -i 'docker-content-digest' | awk '{print $2}' | tr -d '\r'
}

get_latest_ghcr_semver() {
  local image="$1"
  local token
  token=$(curl -sf "https://ghcr.io/token?scope=repository:${image}:pull" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null)
  if [[ -z "$token" ]]; then return 1; fi
  curl -sf -H "Authorization: Bearer $token" "https://ghcr.io/v2/${image}/tags/list" | python3 -c "
import sys, json, re
data = json.load(sys.stdin)
tags = [t for t in data.get('tags', []) if re.match(r'^v?\d+\.\d+\.\d+$', t)]
tags.sort(key=lambda t: list(map(int, re.findall(r'\d+', t))), reverse=True)
if tags:
    print(tags[0])
" 2>/dev/null
}

report() {
  local status="$1"
  local name="$2"
  local message="$3"

  case "$status" in
  ok)
    printf "  ${GREEN}✓${NC} %-35s %s\n" "$name" "$message"
    ((UP_TO_DATE++))
    ;;
  outdated)
    printf "  ${RED}✗${NC} %-35s %s\n" "$name" "$message"
    ((OUTDATED++))
    ;;
  skip)
    printf "  ${YELLOW}⊘${NC} %-35s %s\n" "$name" "$message"
    ((SKIPPED++))
    ;;
  error)
    printf "  ${RED}!${NC} %-35s %s\n" "$name" "$message"
    ((ERRORS++))
    ;;
  esac
}

apply_sed() {
  local old="$1"
  local new="$2"
  local dir="$3"

  if [[ "$APPLY" == true ]]; then
    find "$dir" \( -name "*.groovy" -o -name "*.yaml" \) -exec sed -i "s|${old}|${new}|g" {} +
  fi
}

# =============================================================================
# Container Images
# =============================================================================
#
# All images are resolved through Artifactory mirrors (the same registry
# the pods pull from). The "repo" column is the Artifactory virtual/remote
# repo that proxies the upstream registry.
#
# Format: IMAGES["image/path"]="artifactory-repo tag"
#   artifactory-repo: docker (Docker Hub, GHCR, MCR, JFrog), gi-gantuar-ironbank
#   tag: the tag to resolve

declare -A IMAGES

# Docker Hub images (via artifactory docker remote)
IMAGES["alpine/helm"]="docker latest"
IMAGES["alpine/jmeter"]="docker latest"
IMAGES["snyk/snyk"]="docker node"
IMAGES["golangci/golangci-lint"]="docker latest"
IMAGES["anchore/grype"]="docker latest"
IMAGES["jfrog/jfrog-cli-v2-jf"]="docker latest"
# IronBank images (moved from Docker Hub / GHCR)
IMAGES["ironbank/opensource/tflint/tflint"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/microsoft/playwright/node"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/container-hardening-tools/renovate/renovate"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/sonarsource/sonar-scanner-cli"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/cypress/included"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/amazon/aws-cli"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/afdco/fluxcd/flux-cli"]="gi-gantuar-ironbank latest"
# IronBank images (via artifactory gi-gantuar-ironbank remote)
IMAGES["ironbank/big-bang/base"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/google/golang/ubi9/golang-1.26"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/ironbank-pipelines/oscap-podman"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/alpinelinux/alpine"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/ansible/ansible"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/aquasec/trivy"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/argoproj/argocd"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/clamav/clamav"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/containers/podman"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/gradle/gradle-jdk25"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/hadolint/hadolint"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/maven/maven-openjdk-17"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/maven/maven-openjdk-25"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/openpolicyagent/conftest"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/owasp-zap/owasp-zap"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/python"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/shellcheck/shellcheck"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/trufflehog/trufflehog"]="gi-gantuar-ironbank latest"

echo "Checking container images..."

for image in $(echo "${!IMAGES[@]}" | tr ' ' '\n' | sort); do
  read -r repo tag <<<"${IMAGES[$image]}"

  new_digest=$(get_artifactory_digest "$repo" "$image" "$tag" 2>/dev/null || true)

  if [[ -z "$new_digest" ]]; then
    report error "$image" "failed to resolve digest"
    continue
  fi

  # Find current digest in pod YAML and groovy files
  current_digest=$(grep -rohP "${image}@sha256:[a-f0-9]{64}" "$LIBRARIES_DIR" --include="*.groovy" --include="*.yaml" 2>/dev/null | head -1 | grep -oP 'sha256:[a-f0-9]{64}' || true)

  if [[ -z "$current_digest" ]]; then
    report skip "$image" "not found pinned in libraries"
    continue
  fi

  if [[ "$current_digest" == "$new_digest" ]]; then
    report ok "$image" "up-to-date (${current_digest:0:19}...)"
  else
    report outdated "$image" "${current_digest:0:19}... → ${new_digest:0:19}..."
    apply_sed "${image}@${current_digest}" "${image}@${new_digest}" "$LIBRARIES_DIR"
  fi
done

# =============================================================================
# Summary
# =============================================================================

echo ""
echo "---"
printf "Summary: ${RED}%d outdated${NC}, ${GREEN}%d up-to-date${NC}, ${YELLOW}%d skipped${NC}" "$OUTDATED" "$UP_TO_DATE" "$SKIPPED"
if [[ $ERRORS -gt 0 ]]; then
  printf ", ${RED}%d errors${NC}" "$ERRORS"
fi
echo ""

if [[ $OUTDATED -gt 0 && "$APPLY" == false ]]; then
  echo "Run with --apply to update."
elif [[ $OUTDATED -gt 0 && "$APPLY" == true ]]; then
  echo "Applied $OUTDATED updates."
fi
