#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JMETER_VERSION="5.6.3"
# Apache's published sha512 for apache-jmeter-5.6.3.tgz. Change it together
# with JMETER_VERSION.
JMETER_SHA512="5978a1a35edb5a7d428e270564ff49d2b1b257a65e17a759d259a9283fc17093e522fe46f474a043864aea6910683486340706d745fcdf3db1505fd71e689083"
JMETER_DIR="$SCRIPT_DIR/.jmeter"
JMETER_HOME="$JMETER_DIR/apache-jmeter-$JMETER_VERSION"
JMETER_TGZ="$JMETER_DIR/apache-jmeter-$JMETER_VERSION.tgz"
MIRROR="https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz"

if [[ -x "$JMETER_HOME/bin/jmeter" ]]; then
  echo "JMeter $JMETER_VERSION already installed at $JMETER_HOME"
  exit 0
fi

# JMeter comes from outside JFrog, so it is scanned before use.
if ! command -v clamscan >/dev/null 2>&1; then
  echo "clamscan is required to scan the JMeter download; install ClamAV first." >&2
  exit 1
fi

echo "Downloading Apache JMeter $JMETER_VERSION..."
mkdir -p "$JMETER_DIR"
curl -fSL --proto '=https' --tlsv1.2 "$MIRROR" -o "$JMETER_TGZ"

echo "Verifying checksum..."
if ! echo "$JMETER_SHA512  $JMETER_TGZ" | sha512sum -c --quiet -; then
  rm -f "$JMETER_TGZ"
  echo "Checksum mismatch for $JMETER_TGZ; refusing to install." >&2
  exit 1
fi

echo "Scanning with ClamAV..."
if ! clamscan --no-summary "$JMETER_TGZ"; then
  rm -f "$JMETER_TGZ"
  echo "ClamAV flagged $JMETER_TGZ; refusing to install." >&2
  exit 1
fi

echo "Extracting..."
tar -xzf "$JMETER_TGZ" -C "$JMETER_DIR"
rm -f "$JMETER_TGZ"

echo "JMeter $JMETER_VERSION installed to $JMETER_HOME"
echo "Binary: $JMETER_HOME/bin/jmeter"
