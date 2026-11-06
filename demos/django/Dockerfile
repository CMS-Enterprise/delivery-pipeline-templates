FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/python@sha256:ae054478b8ed4574b7a464fe6791cda96551254cb7d070c116f22ff196594273 AS builder
# hadolint ignore=DL3002
USER 0
WORKDIR /app

COPY requirements.txt ./
COPY . .

RUN /home/python/python-env/bin/pip install --no-cache-dir \
    --index-url https://artifactory.cloud.cms.gov/artifactory/api/pypi/python/simple \
    -r requirements.txt && \
  /home/python/python-env/bin/python manage.py collectstatic --noinput && \
  mkdir -p /app/data && \
  /home/python/python-env/bin/python -m pip uninstall -y pip

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/python@sha256:ae054478b8ed4574b7a464fe6791cda96551254cb7d070c116f22ff196594273
USER 0
# pip vendors its own msgpack, urllib3 and pkg_resources; the runtime never installs packages.
RUN dnf upgrade -y --nodocs && dnf clean all && rm -rf /var/cache/dnf && \
  rm -rf /home/python/python-env /opt/python/3.15.0/lib/python3.15/site-packages/pip*
COPY --from=builder --chown=1001:1001 /home/python/python-env /home/python/python-env
COPY --from=builder --chown=1001:1001 /app /app
USER 1001
WORKDIR /app

ENV PYTHONUNBUFFERED=1 \
  DJANGO_SETTINGS_MODULE=mysite.settings \
  PORT=3003 \
  PATH="/home/python/python-env/bin:${PATH}"

EXPOSE 3003

ENTRYPOINT []
CMD ["sh", "-c", "python manage.py migrate --noinput && gunicorn mysite.wsgi:application --bind 0.0.0.0:3003 --workers 2 --timeout 60"]
