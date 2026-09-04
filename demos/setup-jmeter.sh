#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JMETER_VERSION="5.6.3"
JMETER_DIR="$SCRIPT_DIR/.jmeter"
JMETER_HOME="$JMETER_DIR/apache-jmeter-$JMETER_VERSION"
JMETER_TGZ="$JMETER_DIR/apache-jmeter-$JMETER_VERSION.tgz"
MIRROR="https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz"

if [ -x "$JMETER_HOME/bin/jmeter" ]; then
  echo "JMeter $JMETER_VERSION already installed at $JMETER_HOME"
  exit 0
fi

echo "Downloading Apache JMeter $JMETER_VERSION..."
mkdir -p "$JMETER_DIR"
curl -fSL "$MIRROR" -o "$JMETER_TGZ"

echo "Extracting..."
tar -xzf "$JMETER_TGZ" -C "$JMETER_DIR"
rm -f "$JMETER_TGZ"

echo "JMeter $JMETER_VERSION installed to $JMETER_HOME"
echo "Binary: $JMETER_HOME/bin/jmeter"
