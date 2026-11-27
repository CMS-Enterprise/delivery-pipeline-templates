#!/usr/bin/env bash
# Runs a local lint tool from a pinned container image rather than a binary on
# the workstation, so the hooks and `make` use the same digest-pinned artifacts
# as everything else. Usage: scripts/run-tool.sh <conftest|gitleaks> [args...]
#
# The digests are maintained by scripts/update-pins.sh. Both images come through
# the artifactory Docker Hub mirror because they are multi-arch; the Ironbank
# conftest image the pipeline uses is amd64-only and will not run on an arm64
# workstation. Pull access needs `podman login artifactory.cloud.cms.gov`.
set -eu

CONFTEST_IMAGE="artifactory.cloud.cms.gov/docker/openpolicyagent/conftest@sha256:a38ba21668929a00dce2fe6ee43d1312228340bce5fd243f47dd0ce90516e558"
GITLEAKS_IMAGE="artifactory.cloud.cms.gov/docker/zricethezav/gitleaks@sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f"

tool="${1:?usage: run-tool.sh <conftest|gitleaks> [args...]}"
shift

case "$tool" in
conftest) image="$CONFTEST_IMAGE" ;;
gitleaks) image="$GITLEAKS_IMAGE" ;;
*)
  echo "run-tool: unknown tool '$tool'" >&2
  exit 2
  ;;
esac

# A fresh clone must still be able to commit and push, so a missing container
# runtime skips with a hint rather than failing the hook.
if command -v podman >/dev/null 2>&1; then
  runtime=podman
  # keep-id runs the tool as the invoking user, so git inside the container
  # does not reject the repository for dubious ownership.
  user_args=(--userns=keep-id)
elif command -v docker >/dev/null 2>&1; then
  runtime=docker
  user_args=(--user "$(id -u):$(id -g)")
else
  echo "run-tool: neither podman nor docker is on PATH, skipping $tool." >&2
  exit 0
fi

# Mount the work tree at its own path so file paths in findings match the host.
# A linked worktree keeps its object store in the main repository's git dir,
# which lies outside the work tree, so mount that too when it does.
top="$(git rev-parse --show-toplevel)"
mounts=(-v "$top:$top:ro")
common="$(cd "$(git rev-parse --git-common-dir)" && pwd)"
case "$common/" in
"$top"/*) ;;
*) mounts+=(-v "$common:$common:ro") ;;
esac

exec "$runtime" run --rm --network=none "${user_args[@]}" "${mounts[@]}" \
  -w "$PWD" --entrypoint "$tool" "$image" "$@"
