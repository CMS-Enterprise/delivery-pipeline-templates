#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PIDS_FILE="$SCRIPT_DIR/.pids"

if [ ! -f "$PIDS_FILE" ]; then
    echo "No pids file found. Nothing to stop."
    exit 0
fi

echo "Stopping all webapps..."
while read -r pid project; do
    if kill -0 "$pid" 2>/dev/null; then
        # Kill the entire process group to catch child processes (dev servers spawn subprocesses)
        kill -- -"$pid" 2>/dev/null || kill "$pid" 2>/dev/null || true
        # Wait briefly, then force-kill any survivors
        sleep 1
        if kill -0 "$pid" 2>/dev/null; then
            kill -9 -- -"$pid" 2>/dev/null || kill -9 "$pid" 2>/dev/null || true
        fi
        echo "[$project] Stopped (PID $pid)"
    else
        echo "[$project] Already stopped (PID $pid)"
    fi
done < "$PIDS_FILE"

# Also kill anything still listening on our ports as a fallback
for port in 3000 3001 3002 3003 3004 3005; do
    pid=$(lsof -ti :"$port" 2>/dev/null) || true
    if [ -n "$pid" ]; then
        kill $pid 2>/dev/null || kill -9 $pid 2>/dev/null || true
        echo "[port $port] Killed orphan process (PID $pid)"
    fi
done

rm -f "$PIDS_FILE"
echo
echo "All webapps stopped."
