# Multi-stage Dockerfile for Django demo application
# Uses Artifactory mirrors per project policy with SHA256 digests

# Stage 1: Dependencies
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/python@sha256:ae054478b8ed4574b7a464fe6791cda96551254cb7d070c116f22ff196594273 AS builder

WORKDIR /app

# Copy requirements
COPY requirements.txt ./

# Install dependencies into a known location
RUN pip3 install --no-cache-dir --user -r requirements.txt

# Stage 2: Runtime
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/python@sha256:ae054478b8ed4574b7a464fe6791cda96551254cb7d070c116f22ff196594273

WORKDIR /home/python/app

# Copy installed dependencies from builder
COPY --from=builder /home/python/.local /home/python/.local
ENV PATH="/home/python/.local/bin:${PATH}"

# Copy application code
COPY . .

# Set environment variables
ENV PYTHONUNBUFFERED=1 \
    DJANGO_SETTINGS_MODULE=mysite.settings \
    PORT=3003

# Collect static files (WhiteNoise will serve them)
RUN python3 manage.py collectstatic --noinput

# Django/Gunicorn runs on port 3003
EXPOSE 3003

# Create directory for SQLite database
RUN mkdir -p /home/python/app/data

# Run migrations and start Gunicorn
CMD python3 manage.py migrate --noinput && \
    gunicorn mysite.wsgi:application --bind 0.0.0.0:3003 --workers 2 --timeout 60
