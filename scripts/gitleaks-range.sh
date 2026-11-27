#!/usr/bin/env bash
# Scans only the commits being pushed, rather than all of history, so the check
# stays inside a pre-push time budget. This is the compensating control for
# `git commit --no-verify`: a secret that skipped the pre-commit hook is still
# caught here, before it leaves the workstation.
set -eu

# gitleaks runs from a pinned image; run-tool.sh skips if no runtime exists.
RUN_TOOL="$(dirname "$0")/run-tool.sh"

FROM_REF="${PRE_COMMIT_FROM_REF:-}"
TO_REF="${PRE_COMMIT_TO_REF:-HEAD}"

# Git reports an all-zero from-ref when the remote has no such branch yet, so
# there is no merge base to diff against. Falling back to the default branch
# keeps the first push of a feature branch from scanning the entire history.
if [ -n "$FROM_REF" ] && [ -z "${FROM_REF//0/}" ]; then
  FROM_REF=""
fi

if [ -z "$FROM_REF" ]; then
  DEFAULT_BRANCH="$(git symbolic-ref --quiet --short refs/remotes/origin/HEAD 2>/dev/null || echo origin/main)"
  if git rev-parse --verify --quiet "$DEFAULT_BRANCH" >/dev/null; then
    FROM_REF="$DEFAULT_BRANCH"
  else
    echo "gitleaks-range: no base ref found, scanning working tree only." >&2
    exec "$RUN_TOOL" gitleaks dir --redact --no-banner .
  fi
fi

if [ "$(git rev-list --count "$FROM_REF..$TO_REF")" -eq 0 ]; then
  echo "gitleaks-range: no commits in $FROM_REF..$TO_REF, nothing to scan."
  exit 0
fi

exec "$RUN_TOOL" gitleaks git --redact --no-banner --log-opts="$FROM_REF..$TO_REF" .
