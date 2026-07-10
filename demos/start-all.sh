#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PIDS_FILE="$SCRIPT_DIR/.pids"

PROJECTS=(nextjs vue sveltekit django maven go)

true >"$PIDS_FILE"

echo "Starting webapps..."
echo

for project in "${PROJECTS[@]}"; do
  project_dir="$SCRIPT_DIR/$project"

  if [ ! -d "$project_dir" ]; then
    echo "[$project] SKIP - directory not found at $project_dir"
    continue
  fi

  echo "[$project] Starting webapp..."
  setsid bash -c "cd \"$project_dir\" && exec bash run.sh >>\"$project_dir/$project.log\" 2>&1" &
  pid=$!
  echo "$pid $project" >>"$PIDS_FILE"
  echo "[$project] Started (PID $pid)"
  echo
done

echo "All webapps started:"
echo "  nextjs    -> http://localhost:3000"
echo "  vue       -> http://localhost:3001"
echo "  sveltekit -> http://localhost:3002"
echo "  django    -> http://localhost:3003"
echo "  maven     -> http://localhost:3004"
echo "  go        -> http://localhost:3005"
echo
echo "PIDs saved to $PIDS_FILE"
echo "To stop all: ./stop-all.sh"
