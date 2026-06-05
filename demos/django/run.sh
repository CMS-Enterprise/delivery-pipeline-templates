#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

if [ ! -d venv ]; then
    python3 -m venv venv
fi

source venv/bin/activate
pip install --quiet -r requirements.txt
python manage.py runserver 3003
