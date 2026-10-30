FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/python@sha256:ae054478b8ed4574b7a464fe6791cda96551254cb7d070c116f22ff196594273 AS builder
# hadolint ignore=DL3002
USER 0
WORKDIR /app

COPY requirements.txt ./
COPY . .

RUN /home/python/python-env/bin/pip install --no-cache-dir -r requirements.txt && \
  /home/python/python-env/bin/python manage.py collectstatic --noinput && \
  mkdir -p /app/data

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/alpinelinux/alpine@sha256:bf846ead9f772493743d4b3ee9ef891fcc39c8eb9169ba2564fa4128c63677b6
USER 0
RUN apk update --no-cache && apk upgrade --no-cache
USER 1000
WORKDIR /app

COPY --from=builder /home/python/python-env /home/python/python-env
COPY --from=builder /app .

ENV PYTHONUNBUFFERED=1 \
  DJANGO_SETTINGS_MODULE=mysite.settings \
  PORT=3003 \
  PATH="/home/python/python-env/bin:${PATH}"

EXPOSE 3003

CMD ["sh", "-c", "python manage.py migrate --noinput && gunicorn mysite.wsgi:application --bind 0.0.0.0:3003 --workers 2 --timeout 60"]
