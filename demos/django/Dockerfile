# Multi-stage Dockerfile for Django demo application
# Uses Artifactory mirrors per project policy with SHA256 digests

# Stage 1: Dependencies
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/python@sha256:6ebb415ade9c41f33864663baa10ea70be59bf5ca7effde2daef18612846a690 AS builder
USER 0

WORKDIR /app

# Copy requirements
COPY requirements.txt ./

# Install dependencies into a known location
RUN pip3.14 install --no-cache-dir --prefix=/install -r requirements.txt

# Stage 2: Runtime
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/python@sha256:6ebb415ade9c41f33864663baa10ea70be59bf5ca7effde2daef18612846a690

WORKDIR /app

# Copy installed dependencies from builder
COPY --from=builder /install /usr/local

# Copy application code
COPY . .

# Set environment variables
ENV PYTHONUNBUFFERED=1 \
    DJANGO_SETTINGS_MODULE=mysite.settings \
    PORT=3003

# Collect static files (WhiteNoise will serve them)
RUN python3.14 manage.py collectstatic --noinput

# Django/Gunicorn runs on port 3003
EXPOSE 3003

# Create directory for SQLite database
RUN mkdir -p /app/data

# Run migrations and start Gunicorn
CMD python3.14 manage.py migrate --noinput && \
    gunicorn mysite.wsgi:application --bind 0.0.0.0:3003 --workers 2 --timeout 60
