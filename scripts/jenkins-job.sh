#!/usr/bin/env bash
# Triggers demo builds and reads their output, so a pipeline change can be
# exercised end to end without hand-copying console logs out of a browser.
#
# Requires JENKINS_URL, JENKINS_USER and JENKINS_TOKEN — see .envrc.jenkins.
#
# Job paths are restricted to JENKINS_JOB_PREFIX (default "demos") to keep an
# accidental trigger away from other teams' jobs. This is a guardrail against
# mistakes, NOT a security boundary: the same token can reach every job it has
# permission for via plain curl. Real scoping needs a Jenkins-side service
# account whose build permission is limited to the demos folder.
set -eu

PREFIX="${JENKINS_JOB_PREFIX:-demos}"

usage() {
    cat >&2 <<EOF
usage: $0 <command> [args]

  trigger <job> [KEY=VALUE ...]   Trigger a build, print the queued build number
  status  <job> [build]           Report result and duration (default: last build)
  log     <job> [build]           Print the console log (default: last build)
  tail    <job> [build]           Follow the console log until the build ends

<job> is a path such as "demos" or "demos/my-branch". Multibranch branch names
containing "/" must be URL-encoded by the caller (feature%2Fx).

Only jobs at or under "${PREFIX}" are permitted. Override with JENKINS_JOB_PREFIX.
EOF
}

die() {
    echo "jenkins-job: $*" >&2
    exit 1
}

for v in JENKINS_URL JENKINS_USER JENKINS_TOKEN; do
    eval "val=\${$v:-}"
    [ -n "$val" ] || die "$v is not set. source .envrc.jenkins first."
done

command -v jq >/dev/null 2>&1 || die "jq is required but not on PATH."

BASE="${JENKINS_URL%/}/"
AUTH="--user ${JENKINS_USER}:${JENKINS_TOKEN}"

# Refuse anything outside the allowed prefix before any request is made.
check_prefix() {
    case "$1" in
        "$PREFIX" | "$PREFIX"/*) ;;
        *) die "refusing job '$1': outside allowed prefix '${PREFIX}'." ;;
    esac
}

# "demos/my-branch" -> "job/demos/job/my-branch", the path form the REST API uses.
job_url() {
    printf '%sjob/%s' "$BASE" "$(printf '%s' "$1" | sed 's#/#/job/#g')"
}

crumb() {
    # shellcheck disable=SC2086 # AUTH must word-split into two curl args.
    curl -sS --max-time 15 $AUTH "${BASE}crumbIssuer/api/json" | jq -r '.crumb'
}

# -g (globoff) is required: the API's tree=field[a,b] syntax uses brackets, which
# curl otherwise parses as a glob range and rejects outright.
api() {
    # shellcheck disable=SC2086 # AUTH must word-split into two curl args.
    curl -sS -g --max-time 30 $AUTH "$@"
}

# Resolves the build number for a job, defaulting to the last build.
resolve_build() {
    if [ -n "${2:-}" ]; then
        printf '%s' "$2"
        return
    fi
    n="$(api "$(job_url "$1")/api/json?tree=lastBuild[number]" | jq -r '.lastBuild.number // empty')"
    [ -n "$n" ] || die "job '$1' has no builds yet."
    printf '%s' "$n"
}

cmd_trigger() {
    job="$1"
    shift
    check_prefix "$job"

    C="$(crumb)"
    [ -n "$C" ] && [ "$C" != "null" ] || die "could not fetch a CSRF crumb from ${BASE}."

    set -- ${1+"$@"}
    params=""
    for kv in ${1+"$@"}; do
        case "$kv" in
            *=*) params="$params --data-urlencode ${kv}" ;;
            *) die "parameter '$kv' is not KEY=VALUE." ;;
        esac
    done

    # buildWithParameters answers 201 with a Location header pointing at the
    # queue item. The queue item only gains an executable once an executor picks
    # it up, so the build number is not known immediately.
    # shellcheck disable=SC2086 # AUTH and params must word-split into curl args.
    location="$(curl -sS --max-time 30 $AUTH -H "Jenkins-Crumb:${C}" -D - -o /dev/null \
        $params -X POST "$(job_url "$job")/buildWithParameters" \
        | sed -n 's/^[Ll]ocation: *\(.*\)$/\1/p' | tr -d '\r')"

    [ -n "$location" ] || die "trigger failed for '$job' (no queue Location returned)."
    queue_id="$(printf '%s' "$location" | sed 's#.*/queue/item/\([0-9]*\)/*$#\1#')"
    echo "queued: ${job} (queue item ${queue_id})"

    printf 'waiting for an executor'
    for _ in $(seq 1 60); do
        item="$(api "${BASE}queue/item/${queue_id}/api/json" 2>/dev/null || true)"
        num="$(printf '%s' "$item" | jq -r '.executable.number // empty' 2>/dev/null || true)"
        if [ -n "$num" ]; then
            echo
            echo "build: ${num}"
            echo "url:   $(job_url "$job")/${num}/"
            return 0
        fi
        if printf '%s' "$item" | jq -e '.cancelled == true' >/dev/null 2>&1; then
            echo
            die "queue item ${queue_id} was cancelled."
        fi
        printf '.'
        sleep 2
    done
    echo
    echo "still queued after 120s; check ${BASE}queue/ for why." >&2
}

cmd_status() {
    check_prefix "$1"
    n="$(resolve_build "$@")"
    api "$(job_url "$1")/${n}/api/json?tree=number,building,result,durationMillis,displayName" \
        | jq -r '"build \(.number): \(if .building then "BUILDING" else (.result // "UNKNOWN") end) (\((.durationMillis // 0) / 1000 | floor)s)"'
}

cmd_log() {
    check_prefix "$1"
    n="$(resolve_build "$@")"
    api "$(job_url "$1")/${n}/consoleText"
}

cmd_tail() {
    check_prefix "$1"
    n="$(resolve_build "$@")"
    start=0
    while :; do
        # progressiveText returns the bytes after $start plus an
        # X-More-Data header while the build is still writing.
        headers="$(api -D - -o /tmp/jenkins-tail.$$ \
            "$(job_url "$1")/${n}/logText/progressiveText?start=${start}")"
        cat /tmp/jenkins-tail.$$
        size="$(printf '%s' "$headers" | sed -n 's/^[Xx]-[Tt]ext-[Ss]ize: *\([0-9]*\).*/\1/p' | tail -1)"
        [ -n "$size" ] && start="$size"
        if ! printf '%s' "$headers" | grep -qi '^x-more-data: *true'; then
            rm -f /tmp/jenkins-tail.$$
            break
        fi
        sleep 3
    done
    cmd_status "$1" "$n"
}

case "${1:-}" in
    trigger)
        shift
        [ "$#" -ge 1 ] || { usage; exit 2; }
        cmd_trigger "$@"
        ;;
    status)
        shift
        [ "$#" -ge 1 ] || { usage; exit 2; }
        cmd_status "$@"
        ;;
    log)
        shift
        [ "$#" -ge 1 ] || { usage; exit 2; }
        cmd_log "$@"
        ;;
    tail)
        shift
        [ "$#" -ge 1 ] || { usage; exit 2; }
        cmd_tail "$@"
        ;;
    -h | --help | "")
        usage
        exit 0
        ;;
    *)
        die "unknown command '${1}'. Run with --help."
        ;;
esac
