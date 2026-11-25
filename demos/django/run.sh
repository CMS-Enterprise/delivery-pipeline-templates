#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

if [[ ! -d venv ]]; then
  python3 -m venv venv
fi

# shellcheck disable=SC1091
source venv/bin/activate
# Wheels only: an sdist would run its setup.py during install.
pip3 install --quiet --only-binary :all: -r requirements.txt
python manage.py runserver 3003
