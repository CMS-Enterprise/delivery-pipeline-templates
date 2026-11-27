FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/python@sha256:e1e826d5e8476f03c7ffe183051148528087fca42e86f6dbcfa1e5cb579beec8 AS builder
# hadolint ignore=DL3002
USER 0
WORKDIR /app

COPY requirements.txt ./
COPY . .

# Set by podman's --source-date-epoch. With it set, Python writes hash-based
# .pyc files instead of ones stamped with the source mtime.
ARG SOURCE_DATE_EPOCH
RUN /home/python/python-env/bin/pip install --no-cache-dir \
    --index-url https://artifactory.cloud.cms.gov/artifactory/api/pypi/python/simple \
    -r requirements.txt && \
  /home/python/python-env/bin/python manage.py collectstatic --noinput && \
  mkdir -p /app/data && \
  /home/python/python-env/bin/python -m pip uninstall -y pip

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/python@sha256:e1e826d5e8476f03c7ffe183051148528087fca42e86f6dbcfa1e5cb579beec8
USER 0
# pip vendors its own msgpack, urllib3 and pkg_resources; the runtime never installs packages.
# The glob covers both interpreter layouts the base image has shipped
# (/opt/python/<ver> and /usr/local); the find fails the build if pip survives
# anywhere, so a base image change cannot make the removal silently miss again.
# OS_PATCH_DATE changes daily (jfrogBuildPublish sets it), so a layer cache
# never serves an OS upgrade older than a day.
ARG OS_PATCH_DATE
RUN echo "OS packages as of ${OS_PATCH_DATE:-unset}" && \
  dnf upgrade -y --nodocs && dnf clean all && rm -rf /var/cache/dnf && \
  rm -rf /home/python/python-env \
    /opt/python/*/lib/python3.*/site-packages/pip* \
    /usr/local/lib/python3.*/site-packages/pip* && \
  leftover="$(find / -xdev -type d -path '*/site-packages/pip' -print)" && \
  if [ -n "$leftover" ]; then echo "pip still present: $leftover" >&2; exit 1; fi
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
