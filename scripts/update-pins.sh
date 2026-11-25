#!/usr/bin/env bash
set -uo pipefail

APPLY=false
if [[ "${1:-}" == "--apply" ]]; then APPLY=true; fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$SCRIPT_DIR/.."

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
    "https://${ARTIFACTORY}/v2/token?service=${ARTIFACTORY}&scope=repository:${repo}/${image}:pull" |
    python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null || true)
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

# Files that pin images. Jenkinsfile is included because templates carry inline
# pod specs; leaving it out let those pins drift from resources/pods/.
PIN_FILES=(-name "*.groovy" -o -name "*.yaml" -o -name "Dockerfile" -o -name "Jenkinsfile")

# Rewrites every pin of an image to the new digest, whatever digest each file
# currently holds, so pins that have diverged are brought back together.
apply_sed() {
  local image="$1"
  local new="$2"
  local dir="$3"

  if [[ "$APPLY" == true ]]; then
    find "$dir" -path "$dir/.git" -prune -o -type f \( "${PIN_FILES[@]}" \) -print0 |
      xargs -0 sed -i -E "s|${image//./\\.}@sha256:[a-f0-9]{64}|${image}@${new}|g"
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
IMAGES["anchore/grype"]="docker debug"
IMAGES["jfrog/jfrog-cli-v2-jf"]="docker latest"
# IronBank images (moved from Docker Hub / GHCR)
IMAGES["ironbank/opensource/tflint/tflint"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/microsoft/playwright/node"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/container-hardening-tools/renovate/renovate"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/sonarsource/sonar-scanner-cli"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/cypress/included"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/amazon/aws-cli"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/afdco/fluxcd/flux-cli"]="gi-gantuar-ironbank v2.9.5"
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
IMAGES["ironbank/opensource/nginx/nginx"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/nodejs/debian/nodejs"]="gi-gantuar-ironbank 26-npm12-yarn4"
IMAGES["ironbank/hashicorp/terraform"]="gi-gantuar-ironbank 1.15"
IMAGES["ironbank/opensource/python"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/shellcheck/shellcheck"]="gi-gantuar-ironbank latest"
IMAGES["ironbank/opensource/trufflehog/trufflehog"]="gi-gantuar-ironbank 3.99.7"
IMAGES["ironbank/redhat/openjdk/openjdk25-runtime-ubi9-slim"]="gi-gantuar-ironbank latest"

echo "Checking container images..."

for image in $(echo "${!IMAGES[@]}" | tr ' ' '\n' | sort); do
  read -r repo tag <<<"${IMAGES[$image]}"

  new_digest=$(get_artifactory_digest "$repo" "$image" "$tag" 2>/dev/null || true)

  if [[ -z "$new_digest" ]]; then
    report error "$image" "failed to resolve digest"
    continue
  fi

  # Every distinct digest the image is pinned at, across pod YAML, groovy,
  # Dockerfiles and Jenkinsfiles. More than one means the pins have diverged.
  current_digests=$(grep -rohP "${image}@sha256:[a-f0-9]{64}" "$REPO_ROOT" --exclude-dir=.git \
    --include="*.groovy" --include="*.yaml" --include="Dockerfile" --include="Jenkinsfile" 2>/dev/null |
    grep -oP 'sha256:[a-f0-9]{64}' | sort -u || true)

  if [[ -z "$current_digests" ]]; then
    report skip "$image" "not found pinned in libraries"
    continue
  fi

  if [[ "$current_digests" == "$new_digest" ]]; then
    report ok "$image" "up-to-date (${new_digest:0:19}...)"
  else
    olds=$(grep -vxF "$new_digest" <<<"$current_digests" | cut -c1-19 | paste -sd, -)
    report outdated "$image" "${olds}... → ${new_digest:0:19}..."
    apply_sed "$image" "$new_digest" "$REPO_ROOT"
  fi
done

# =============================================================================
# npm tarballs pinned by sha512
# =============================================================================
#
# Dockerfiles that upgrade npm run `npm pack npm@X.Y.Z` and check the tarball
# against the npm.sha512 beside them before installing it. Each pin tracks the
# latest release on its major. The abbreviated packument carries dist.integrity,
# the same sha512 npm itself verifies, so the hash is taken from the registry the
# image build fetches from (Artifactory), falling back to the public registry.

NPM_REGISTRY="${NPM_REGISTRY:-https://${ARTIFACTORY}/artifactory/api/npm/npm}"

echo ""
echo "Checking npm tarball pins..."

npm_meta=""
for registry in "$NPM_REGISTRY" "https://registry.npmjs.org"; do
  npm_meta=$(curl -sf --max-time 30 -H "Accept: application/vnd.npm.install-v1+json" \
    "${registry%/}/npm" 2>/dev/null || true)
  [[ -n "$npm_meta" ]] && break
done

while IFS= read -r -d '' sumfile; do
  dir=$(dirname "$sumfile")
  name="npm (${dir#"$REPO_ROOT"/})"
  dockerfile="$dir/Dockerfile"
  cur_hash=$(awk '{print $1}' "$sumfile")
  cur_ver=$(grep -oP 'npm-\K[0-9]+\.[0-9]+\.[0-9]+(?=\.tgz)' "$sumfile")

  if [[ -z "$cur_ver" ]] || ! grep -qF "npm@${cur_ver}" "$dockerfile" 2>/dev/null; then
    report error "$name" "npm.sha512 and Dockerfile disagree on the npm version"
    continue
  fi
  if [[ -z "$npm_meta" ]]; then
    report error "$name" "failed to fetch npm metadata"
    continue
  fi

  read -r new_ver new_hash < <(python3 -c '
import base64, json, re, sys
major = sys.argv[1]
versions = json.load(sys.stdin)["versions"]
rel = [v for v in versions if re.fullmatch(major + r"\.\d+\.\d+", v)]
best = max(rel, key=lambda v: tuple(map(int, v.split("."))))
integrity = versions[best]["dist"]["integrity"]
assert integrity.startswith("sha512-")
print(best, base64.b64decode(integrity[7:]).hex())
' "${cur_ver%%.*}" <<<"$npm_meta" 2>/dev/null)

  if [[ -z "$new_hash" ]]; then
    report error "$name" "failed to resolve latest ${cur_ver%%.*}.x"
  elif [[ "$new_ver" == "$cur_ver" && "$new_hash" == "$cur_hash" ]]; then
    report ok "$name" "up-to-date (${cur_ver}, sha512:${cur_hash:0:12}...)"
  else
    report outdated "$name" "${cur_ver} → ${new_ver} (sha512:${new_hash:0:12}...)"
    if [[ "$APPLY" == true ]]; then
      # Rewrite in place so the tarball path the Dockerfile checks is kept.
      sed -i -E "s|^[a-f0-9]{128}  (.*)npm-${cur_ver//./\\.}\\.tgz$|${new_hash}  \\1npm-${new_ver}.tgz|" "$sumfile"
      sed -i -e "s|npm@${cur_ver//./\\.}|npm@${new_ver}|g" \
        -e "s|npm-${cur_ver//./\\.}\\.tgz|npm-${new_ver}.tgz|g" "$dockerfile"
    fi
  fi
done < <(find "$REPO_ROOT" -path "$REPO_ROOT/.git" -prune -o -path "*/node_modules" -prune \
  -o -type f -name npm.sha512 -print0)

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
