# Multi-stage Dockerfile for Django demo application
# Uses Artifactory mirrors per project policy with SHA256 digests

# Stage 1: Build
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/python@sha256:ae054478b8ed4574b7a464fe6791cda96551254cb7d070c116f22ff196594273 AS builder
USER 0

WORKDIR /app

# Copy requirements and application code
COPY requirements.txt ./
COPY . .

# Install dependencies and collect static files
RUN /home/python/python-env/bin/pip install --no-cache-dir -r requirements.txt && \
    python manage.py collectstatic --noinput && \
    mkdir -p /app/data

# Stage 2: Runtime
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/python@sha256:ae054478b8ed4574b7a464fe6791cda96551254cb7d070c116f22ff196594273

WORKDIR /app

# Copy fully prepared application from builder
COPY --from=builder /home/python/python-env /home/python/python-env
COPY --from=builder /app .

# Set environment variables
ENV PYTHONUNBUFFERED=1 \
    DJANGO_SETTINGS_MODULE=mysite.settings \
    PORT=3003

# Django/Gunicorn runs on port 3003
EXPOSE 3003

# Run migrations and start Gunicorn
ENV PATH="/home/python/python-env/bin:${PATH}"

CMD python manage.py migrate --noinput && \
    gunicorn mysite.wsgi:application --bind 0.0.0.0:3003 --workers 2 --timeout 60
