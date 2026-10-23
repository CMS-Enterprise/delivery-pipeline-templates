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

get_digest() {
  local ref="$1"

  local digest
  digest=$(podman manifest inspect "$ref" 2>/dev/null | python3 -c "
import sys, json
data = json.load(sys.stdin)
manifests = data.get('manifests', [])
if manifests:
    for m in manifests:
        p = m.get('platform', {})
        if p.get('architecture') == 'amd64' and p.get('os') == 'linux':
            print(m['digest'])
            sys.exit(0)
    print(manifests[0]['digest'])
" 2>/dev/null || true)

  if [[ -z "$digest" ]]; then
    digest=$(podman manifest inspect "$ref" 2>/dev/null | python3 -c "
import sys, json
data = json.load(sys.stdin)
if 'config' in data:
    print(data.get('config', {}).get('digest', ''))
" 2>/dev/null || true)
  fi

  echo "$digest"
}

get_dockerhub_digest() {
  local image="$1"
  local tag="$2"
  if [[ "$image" != */* ]]; then image="library/$image"; fi
  curl -sf "https://auth.docker.io/token?service=registry.docker.io&scope=repository:${image}:pull" | python3 -c "
import sys, json, urllib.request
token = json.load(sys.stdin)['token']
req = urllib.request.Request('https://registry-1.docker.io/v2/${image}/manifests/${tag}',
    headers={'Authorization': f'Bearer {token}',
             'Accept': 'application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.docker.distribution.manifest.v2+json'})
resp = urllib.request.urlopen(req)
print(resp.headers.get('Docker-Content-Digest', ''))
" 2>/dev/null || true
}

get_ghcr_digest() {
  local image="$1"
  local tag="$2"
  local token
  token=$(curl -sf "https://ghcr.io/token?scope=repository:${image}:pull" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null || true)
  if [[ -z "$token" ]]; then return; fi
  curl -sf -H "Authorization: Bearer $token" \
    -H "Accept: application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json" \
    "https://ghcr.io/v2/${image}/manifests/${tag}" | python3 -c "
import sys, json
data = json.load(sys.stdin)
manifests = data.get('manifests', [])
if manifests:
    for m in manifests:
        p = m.get('platform', {})
        if p.get('architecture') == 'amd64' and p.get('os') == 'linux':
            print(m['digest'])
            sys.exit(0)
    print(manifests[0]['digest'])
" 2>/dev/null || true
}

get_ironbank_digest() {
  local image="$1"
  local tag="$2"
  local registry="${IRONBANK_REGISTRY:-artifactory.cloud.cms.gov}"
  local repo="gi-gantuar-ironbank"
  local token
  token=$(curl -sf --max-time 15 \
    "https://${registry}/v2/token?service=${registry}&scope=repository:${repo}/${image}:pull" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null || true)
  if [[ -z "$token" ]]; then return; fi
  curl -sf --max-time 15 \
    -H "Authorization: Bearer ${token}" \
    -H "Accept: application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.v2+json" \
    "https://${registry}/v2/${repo}/${image}/manifests/${tag}" \
    -D /dev/stderr -o /dev/null 2>&1 | grep -i 'docker-content-digest' | awk '{print $2}' | tr -d '\r'
}

get_jfrog_digest() {
  local image="$1"
  local tag="$2"
  local registry="releases-docker.jfrog.io"
  local token
  token=$(curl -sf --max-time 15 \
    "https://${registry}/v2/token?service=${registry}&scope=repository:${image}:pull" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null || true)
  if [[ -z "$token" ]]; then return; fi
  curl -sf --max-time 15 \
    -H "Authorization: Bearer ${token}" \
    -H "Accept: application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.oci.image.index.v1+json" \
    "https://${registry}/v2/${image}/manifests/${tag}" \
    -D /dev/stderr -o /dev/null 2>&1 | grep -i 'docker-content-digest' | awk '{print $2}' | tr -d '\r'
}

get_quay_digest() {
  local image="$1"
  local tag="$2"
  curl -sf "https://quay.io/api/v1/repository/${image}/tag/?specificTag=${tag}" | python3 -c "
import sys, json
data = json.load(sys.stdin)
tags = data.get('tags', [])
if tags:
    print(tags[0].get('manifest_digest', ''))
" 2>/dev/null || true
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

declare -A IMAGES
# Format: IMAGES["image/name"]="registry tag"
# registry: dockerhub, quay, ghcr, mcr
# tag: the tag to resolve (latest, semver, etc.)
IMAGES["alpine/helm"]="dockerhub latest"
IMAGES["cypress/included"]="dockerhub latest"
IMAGES["justb4/jmeter"]="dockerhub latest"
IMAGES["node"]="dockerhub 20"
IMAGES["renovate/renovate"]="dockerhub latest"
IMAGES["snyk/snyk"]="dockerhub node"
IMAGES["sonarsource/sonar-scanner-cli"]="dockerhub latest"
IMAGES["amazon/aws-cli"]="dockerhub latest"
IMAGES["golangci/golangci-lint"]="dockerhub latest"
IMAGES["anchore/grype"]="dockerhub latest"
IMAGES["terraform-linters/tflint"]="ghcr latest"
IMAGES["fluxcd/flux-cli"]="ghcr-semver latest"
IMAGES["playwright"]="mcr latest"
IMAGES["jfrog/jfrog-cli-v2-jf"]="jfrog latest"
# IronBank images (via Artifactory remote-cache)
IMAGES["ironbank/big-bang/base"]="ironbank latest"
IMAGES["ironbank/google/golang/ubi9/golang-1.26"]="ironbank latest"
IMAGES["ironbank/ironbank-pipelines/oscap-podman"]="ironbank latest"
IMAGES["ironbank/opensource/alpinelinux/alpine"]="ironbank latest"
IMAGES["ironbank/opensource/ansible/ansible"]="ironbank latest"
IMAGES["ironbank/opensource/aquasec/trivy"]="ironbank latest"
IMAGES["ironbank/opensource/argoproj/argocd"]="ironbank latest"
IMAGES["ironbank/opensource/clamav/clamav"]="ironbank latest"
IMAGES["ironbank/opensource/containers/podman"]="ironbank latest"
IMAGES["ironbank/opensource/gradle/gradle-jdk25"]="ironbank latest"
IMAGES["ironbank/opensource/hadolint/hadolint"]="ironbank latest"
IMAGES["ironbank/opensource/maven/maven-openjdk-17"]="ironbank latest"
IMAGES["ironbank/opensource/maven/maven-openjdk-25"]="ironbank latest"
IMAGES["ironbank/opensource/openpolicyagent/conftest"]="ironbank latest"
IMAGES["ironbank/opensource/owasp-zap/owasp-zap"]="ironbank latest"
IMAGES["ironbank/opensource/python"]="ironbank latest"
IMAGES["ironbank/opensource/shellcheck/shellcheck"]="ironbank latest"
IMAGES["ironbank/opensource/trufflehog/trufflehog"]="ironbank latest"

get_latest_ghcr_semver() {
  local image="$1"
  local token
  token=$(curl -sf "https://ghcr.io/token?scope=repository:${image}:pull" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" 2>/dev/null)
  if [[ -z "$token" ]]; then return 1; fi

  local tag
  tag=$(curl -sf -H "Authorization: Bearer $token" "https://ghcr.io/v2/${image}/tags/list" | python3 -c "
import sys, json, re
data = json.load(sys.stdin)
tags = [t for t in data.get('tags', []) if re.match(r'^v?\d+\.\d+\.\d+$', t)]
tags.sort(key=lambda t: list(map(int, re.findall(r'\d+', t))), reverse=True)
if tags:
    print(tags[0])
" 2>/dev/null)
  echo "$tag"
}

echo "Checking container images..."

for image in $(echo "${!IMAGES[@]}" | tr ' ' '\n' | sort); do
  read -r registry tag <<<"${IMAGES[$image]}"

  if [[ "$registry" == "private" ]]; then
    report skip "$image" "private registry (requires auth)"
    continue
  fi

  # Determine the full reference for podman
  case "$registry" in
  dockerhub)
    if [[ "$image" != */* ]]; then
      ref="docker.io/library/${image}:${tag}"
    else
      ref="docker.io/${image}:${tag}"
    fi
    ;;
  quay)
    ref="quay.io/${image}:${tag}"
    ;;
  ghcr)
    ref="ghcr.io/${image}:${tag}"
    ;;
  ghcr-semver)
    semver_tag=$(get_latest_ghcr_semver "$image" 2>/dev/null)
    if [[ -z "$semver_tag" ]]; then
      report error "$image" "failed to resolve latest semver tag"
      continue
    fi
    ref="ghcr.io/${image}:${semver_tag}"
    tag="$semver_tag"
    ;;
  gcr)
    ref="gcr.io/${image}:${tag}"
    ;;
  mcr)
    ref="mcr.microsoft.com/${image}:${tag}"
    ;;
  jfrog)
    ref=""
    ;;
  ironbank)
    ref=""
    ;;
  esac

  if [[ "$registry" == "ironbank" ]]; then
    new_digest=$(get_ironbank_digest "$image" "$tag" 2>/dev/null || true)
  elif [[ "$registry" == "jfrog" ]]; then
    new_digest=$(get_jfrog_digest "$image" "$tag" 2>/dev/null || true)
  else
    new_digest=$(get_digest "$ref" 2>/dev/null || true)
  fi

  if [[ -z "$new_digest" ]]; then
    case "$registry" in
    dockerhub)
      new_digest=$(get_dockerhub_digest "$image" "$tag")
      ;;
    quay)
      new_digest=$(get_quay_digest "$image" "$tag")
      ;;
    ghcr | ghcr-semver)
      new_digest=$(get_ghcr_digest "$image" "$tag")
      ;;
    mcr)
      new_digest=$(get_digest "mcr.microsoft.com/${image}:${tag}" 2>/dev/null || true)
      ;;
    jfrog)
      new_digest=$(get_jfrog_digest "$image" "$tag" 2>/dev/null || true)
      ;;
    ironbank)
      ;;
    esac
  fi

  if [[ -z "$new_digest" ]]; then
    report error "$image" "failed to resolve digest"
    continue
  fi

  # Find current digest in .groovy files
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
