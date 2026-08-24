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

get_pypi_version() {
  local package="$1"
  curl -sf "https://pypi.org/pypi/${package}/json" | python3 -c "import sys,json; print(json.load(sys.stdin)['info']['version'])"
}

get_github_sha() {
  local repo="$1"
  local branch="${2:-master}"
  curl -sf "https://api.github.com/repos/${repo}/commits/${branch}" | python3 -c "import sys,json; print(json.load(sys.stdin)['sha'])"
}

get_alpine_pkg_version() {
  local package="$1"
  curl -sf "https://dl-cdn.alpinelinux.org/alpine/edge/community/x86_64/" | grep -oP "${package}-\K[0-9]+\.[0-9]+\.[0-9]+-r[0-9]+" | head -1
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
    find "$dir" -name "*.groovy" -exec sed -i "s|${old}|${new}|g" {} +
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
IMAGES["library/alpine"]="dockerhub latest"
IMAGES["argoproj/argocd"]="dockerhub latest"
IMAGES["bitnami/kubectl"]="dockerhub latest"
IMAGES["clamav/clamav"]="dockerhub latest"
IMAGES["curlimages/curl"]="dockerhub latest"
IMAGES["cypress/included"]="dockerhub latest"
IMAGES["cytopia/ansible"]="dockerhub latest"
IMAGES["projectsigstore/cosign"]="gcr latest"
IMAGES["gradle"]="dockerhub 8-jdk21"
IMAGES["hadolint/hadolint"]="dockerhub latest"
IMAGES["justb4/jmeter"]="dockerhub latest"
IMAGES["koalaman/shellcheck-alpine"]="dockerhub latest"
IMAGES["maven"]="dockerhub 3-eclipse-temurin-21"
IMAGES["node"]="dockerhub 20"
IMAGES["podman/stable"]="quay latest"
IMAGES["renovate/renovate"]="dockerhub latest"
IMAGES["snyk/snyk"]="dockerhub node"
IMAGES["sonarsource/sonar-scanner-cli"]="dockerhub latest"
IMAGES["amazon/aws-cli"]="dockerhub latest"
IMAGES["golangci/golangci-lint"]="dockerhub latest"
IMAGES["anchore/grype"]="dockerhub latest"
IMAGES["aquasec/trivy"]="dockerhub latest"
IMAGES["compliance-as-code/openscap"]="quay latest"
IMAGES["terraform-linters/tflint"]="ghcr latest"
IMAGES["fluxcd/flux-cli"]="ghcr-semver latest"
IMAGES["playwright"]="mcr latest"
IMAGES["twistlock/defender"]="private latest"
IMAGES["jfrog/jfrog-cli-v2"]="jfrog latest"

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
    ref="releases-docker.jfrog.io/${image}:${tag}"
    ;;
  esac

  new_digest=$(get_digest "$ref" 2>/dev/null || true)

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
      new_digest=$(get_digest "releases-docker.jfrog.io/${image}:${tag}" 2>/dev/null || true)
      ;;
    esac
  fi

  if [[ -z "$new_digest" ]]; then
    report error "$image" "failed to resolve digest"
    continue
  fi

  # Find current digest in .groovy files
  current_digest=$(grep -rohP "${image}@sha256:[a-f0-9]{64}" "$LIBRARIES_DIR" --include="*.groovy" 2>/dev/null | head -1 | grep -oP 'sha256:[a-f0-9]{64}' || true)

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
# pip packages
# =============================================================================

echo ""
echo "Checking pip packages..."

declare -A PIP_PACKAGES
PIP_PACKAGES["pytest"]="libraries/pytest/pytest_test.groovy"
PIP_PACKAGES["pytest-cov"]="libraries/pytest/pytest_test.groovy"
PIP_PACKAGES["pylint"]="libraries/pylint/pylint_lint.groovy"

for package in $(echo "${!PIP_PACKAGES[@]}" | tr ' ' '\n' | sort); do
  file="${LIBRARIES_DIR}/../${PIP_PACKAGES[$package]}"

  new_version=$(get_pypi_version "$package" 2>/dev/null || true)
  if [[ -z "$new_version" ]]; then
    report error "$package" "failed to query PyPI"
    continue
  fi

  current_version=$(grep -oP "${package}==\K[0-9]+\.[0-9]+\.[0-9]+" "$file" 2>/dev/null || true)
  if [[ -z "$current_version" ]]; then
    report skip "$package" "no pinned version found"
    continue
  fi

  if [[ "$current_version" == "$new_version" ]]; then
    report ok "$package" "up-to-date ($current_version)"
  else
    report outdated "$package" "$current_version → $new_version"
    if [[ "$APPLY" == true ]]; then
      sed -i "s|${package}==${current_version}|${package}==${new_version}|g" "$file"
    fi
  fi
done

# =============================================================================
# apk packages
# =============================================================================

echo ""
echo "Checking apk packages..."

declare -A APK_PACKAGES
APK_PACKAGES["kustomize"]="libraries/gitops/promote.groovy"

for package in $(echo "${!APK_PACKAGES[@]}" | tr ' ' '\n' | sort); do
  file="${LIBRARIES_DIR}/../${APK_PACKAGES[$package]}"

  new_version=$(get_alpine_pkg_version "$package" 2>/dev/null || true)
  if [[ -z "$new_version" ]]; then
    report error "$package" "failed to query Alpine packages"
    continue
  fi

  current_version=$(grep -oP "${package}=\K[0-9]+\.[0-9]+\.[0-9]+-r[0-9]+" "$file" 2>/dev/null || true)
  if [[ -z "$current_version" ]]; then
    report skip "$package" "no pinned version found"
    continue
  fi

  if [[ "$current_version" == "$new_version" ]]; then
    report ok "$package" "up-to-date ($current_version)"
  else
    report outdated "$package" "$current_version → $new_version"
    if [[ "$APPLY" == true ]]; then
      sed -i "s|${package}=${current_version}|${package}=${new_version}|g" "$file"
    fi
  fi
done

# =============================================================================
# GitHub repos
# =============================================================================

echo ""
echo "Checking GitHub repos..."

declare -A GITHUB_REPOS
# Format: GITHUB_REPOS["owner/repo"]="branch file"
GITHUB_REPOS["tfutils/tfenv"]="master libraries/terraform/deploy.groovy"

for repo in $(echo "${!GITHUB_REPOS[@]}" | tr ' ' '\n' | sort); do
  read -r branch file <<<"${GITHUB_REPOS[$repo]}"
  file="${LIBRARIES_DIR}/../${file}"

  new_sha=$(get_github_sha "$repo" "$branch" 2>/dev/null || true)
  if [[ -z "$new_sha" ]]; then
    report error "$repo" "failed to query GitHub API"
    continue
  fi

  current_sha=$(grep -oP 'git checkout \K[a-f0-9]{40}' "$file" 2>/dev/null || true)
  if [[ -z "$current_sha" ]]; then
    report skip "$repo" "no pinned SHA found"
    continue
  fi

  if [[ "$current_sha" == "$new_sha" ]]; then
    report ok "$repo" "up-to-date (${current_sha:0:12}...)"
  else
    report outdated "$repo" "${current_sha:0:12}... → ${new_sha:0:12}..."
    if [[ "$APPLY" == true ]]; then
      sed -i "s|${current_sha}|${new_sha}|g" "$file"
    fi
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
