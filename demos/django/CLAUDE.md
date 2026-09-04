# Project Setup

- Python 3.10+ required
- Django 5.2 LTS — 4.2 crashes on Python 3.14, which the CI pod uses
- SQLite for development database
- Runtime deps are Django and Gunicorn only; test deps are in `requirements-dev.txt`

# Build & Test

- `pip install -r requirements.txt` - install runtime dependencies
- `pip install -r requirements-dev.txt` - install test dependencies too
- `python manage.py runserver 3003` - start dev server on port 3003
- `pytest tests/` - run tests (pytest-django; config in `pytest.ini`)
- `python manage.py migrate` - apply database migrations
- `./run.sh` - create venv, install deps, and start dev server

# Code Style

- 4-space indentation (PEP 8)
- Function-based views (no class-based views in this project)
- Template inheritance with `{% extends "base.html" %}`
- Keep views thin — business logic belongs in separate modules

# Architecture

- `mysite/` - Django project config (settings, urls, wsgi, asgi)
- `pages/` - Main app with views, urls, and app config
- `pages/templates/` - HTML templates (base.html + pages/)
- `pages/templates/base.html` - Base layout with nav and footer
- `pages/templates/pages/` - Page-specific templates (home.html, about.html)
- `static/css/style.css` - Global stylesheet with CSS variables
- `tests/` - pytest suite (test_urls, test_views, test_templates) plus `tests/jmeter/`

# Workflow

- Create a feature branch: `git checkout -b feature/your-feature-name`
- Run `pytest tests/` before committing
- Include the user prompt in commit messages
- Write tests alongside implementation

# Gotchas

- SECRET_KEY in settings.py is a placeholder — must be changed for production
- Static files served via Django in debug mode only
- The site brand is "RoboCare Health" (robotic healthcare company)
- Templates use Django template language (not Jinja2)
