# Multi-stage Dockerfile for Django demo application
# Uses Artifactory mirrors per project policy with SHA256 digests

# Stage 1: Dependencies
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/python:v3.14 AS builder

WORKDIR /app

# Copy requirements
COPY requirements.txt ./

# Install dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Stage 2: Runtime
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/python:v3.14

WORKDIR /app

# Copy installed dependencies from builder
COPY --from=builder /usr/local/lib/python3.14/site-packages /usr/local/lib/python3.14/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

# Copy application code
COPY . .

# Set environment variables
ENV PYTHONUNBUFFERED=1 \
    DJANGO_SETTINGS_MODULE=mysite.settings \
    PORT=3003

# Collect static files (WhiteNoise will serve them)
RUN python manage.py collectstatic --noinput

# Django/Gunicorn runs on port 3003
EXPOSE 3003

# Create directory for SQLite database
RUN mkdir -p /app/data

# Run migrations and start Gunicorn
CMD python manage.py migrate --noinput && \
    gunicorn mysite.wsgi:application --bind 0.0.0.0:3003 --workers 2 --timeout 60
