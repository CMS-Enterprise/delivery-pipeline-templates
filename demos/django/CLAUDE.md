# Project Setup

- Python 3.10+ required
- Django 4.2
- SQLite for development database
- No additional dependencies beyond Django and Gunicorn

# Build & Test

- `pip install -r requirements.txt` - install dependencies
- `python manage.py runserver 3003` - start dev server on port 3003
- `python manage.py test` - run tests
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
- `templates/` - HTML templates (base.html + pages/)
- `templates/base.html` - Base layout with nav and footer
- `templates/pages/` - Page-specific templates (home.html, about.html)
- `static/css/style.css` - Global stylesheet with CSS variables

# Workflow

- Create a feature branch: `git checkout -b feature/your-feature-name`
- Run `python manage.py test` before committing
- Include the user prompt in commit messages
- Write tests alongside implementation

# Gotchas

- SECRET_KEY in settings.py is a placeholder — must be changed for production
- Static files served via Django in debug mode only
- The site brand is "RoboCare Health" (robotic healthcare company)
- Templates use Django template language (not Jinja2)
