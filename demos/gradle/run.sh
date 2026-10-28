#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
gradle bootRun --console=plain
