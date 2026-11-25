#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
# Install exactly what the lock file pins (with its integrity hashes),
# without running dependency lifecycle scripts.
npm ci --ignore-scripts
npm run dev
