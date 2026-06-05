#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/jmeter-results"
JMETER_HOME="$SCRIPT_DIR/.jmeter/apache-jmeter-5.6.3"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

PROJECTS=(nextjs vue sveltekit django maven go)
PORTS=(3000 3001 3002 3003 3004 3005)

if [ -x "$JMETER_HOME/bin/jmeter" ]; then
  JMETER="$JMETER_HOME/bin/jmeter"
elif command -v jmeter &>/dev/null; then
  echo "WARNING: Using system JMeter (may have XStream compatibility issues)."
  echo "Run ./setup-jmeter.sh to install a compatible local version."
  JMETER="jmeter"
else
  echo "ERROR: JMeter not found."
  echo "Run ./setup-jmeter.sh to install Apache JMeter 5.6.3 locally."
  exit 1
fi

mkdir -p "$RESULTS_DIR"

echo "Running JMeter tests against all projects..."
echo "Results will be saved to: $RESULTS_DIR"
echo

failed=0
passed=0

for i in "${!PROJECTS[@]}"; do
  project="${PROJECTS[$i]}"
  port="${PORTS[$i]}"
  test_plan="$SCRIPT_DIR/$project/tests/jmeter/test-plan.jmx"
  result_file="$RESULTS_DIR/${project}_${TIMESTAMP}.jtl"
  log_file="$RESULTS_DIR/${project}_${TIMESTAMP}.log"

  if [ ! -f "$test_plan" ]; then
    echo "[$project] SKIP - test plan not found at $test_plan"
    continue
  fi

  # Check if the service is reachable
  if ! curl -s --max-time 2 "http://localhost:$port/" >/dev/null 2>&1; then
    echo "[$project] SKIP - service not reachable on port $port"
    ((failed++)) || true
    continue
  fi

  echo "[$project] Running tests (port $port)..."
  if "$JMETER" -n -t "$test_plan" -l "$result_file" -j "$log_file" \
    -JHOST=localhost -JPORT="$port" >/dev/null 2>&1; then
    # Check for assertion failures in results
    fail_count=$(grep -c ",false," "$result_file" 2>/dev/null || echo "0")
    total_count=$(wc -l <"$result_file" 2>/dev/null || echo "0")
    total_count=$((total_count - 1)) # subtract header line
    if [ "$fail_count" -eq 0 ]; then
      echo "[$project] PASSED ($total_count samples, 0 failures)"
      ((passed++)) || true
    else
      echo "[$project] FAILED ($total_count samples, $fail_count failures)"
      ((failed++)) || true
    fi
  else
    echo "[$project] ERROR - JMeter execution failed (see $log_file)"
    ((failed++)) || true
  fi
done

echo "========================================="
echo "Results: $passed passed, $failed failed"
echo "Details: $RESULTS_DIR"
echo "========================================="

exit $failed
