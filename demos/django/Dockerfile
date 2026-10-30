FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/python@sha256:ae054478b8ed4574b7a464fe6791cda96551254cb7d070c116f22ff196594273 AS builder

WORKDIR /app

COPY requirements.txt ./
COPY . .

RUN /home/python/python-env/bin/pip install --no-cache-dir -r requirements.txt && \
  python manage.py collectstatic --noinput && \
  mkdir -p /app/data

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/python@sha256:ae054478b8ed4574b7a464fe6791cda96551254cb7d070c116f22ff196594273
USER 0
RUN dnf upgrade -y --nodocs && dnf clean all || microdnf update -y && microdnf clean all || true
USER 1001

WORKDIR /app

COPY --from=builder /home/python/python-env /home/python/python-env
COPY --from=builder /app .

ENV PYTHONUNBUFFERED=1 \
  DJANGO_SETTINGS_MODULE=mysite.settings \
  PORT=3003

EXPOSE 3003
ENV PATH="/home/python/python-env/bin:${PATH}"

USER 1001
CMD ["sh", "-c", "python manage.py migrate --noinput && gunicorn mysite.wsgi:application --bind 0.0.0.0:3003 --workers 2 --timeout 60"]
