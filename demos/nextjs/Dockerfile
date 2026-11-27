FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/debian/nodejs@sha256:90b09acffe6c620be153311f11d50d72427ab08581de2c7344de9ec7d820a9dc AS builder
# hadolint ignore=DL3002
USER 0
# OS_PATCH_DATE changes daily (jfrogBuildPublish sets it), so a layer cache
# never serves an OS upgrade older than a day.
ARG OS_PATCH_DATE
RUN echo "OS packages as of ${OS_PATCH_DATE:-unset}" && \
  apt-get -o APT::Sandbox::User=root update && \
  apt-get -o APT::Sandbox::User=root -y upgrade && \
  rm -rf /var/lib/apt/lists/*
WORKDIR /home/node/app

COPY --chown=node:node package*.json .npmrc ./
RUN npm ci --ignore-scripts

COPY --chown=node:node . .
RUN npm run build

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/debian/nodejs@sha256:90b09acffe6c620be153311f11d50d72427ab08581de2c7344de9ec7d820a9dc
USER 0
# npm is installed from a tarball checked against the static sha512 in
# npm.sha512 (updated by scripts/update-pins.sh), not trusted by version alone.
COPY npm.sha512 /tmp/npm-pin/
# OS_PATCH_DATE changes daily (jfrogBuildPublish sets it), so a layer cache
# never serves an OS upgrade older than a day.
ARG OS_PATCH_DATE
RUN echo "OS packages as of ${OS_PATCH_DATE:-unset}" && \
  apt-get -o APT::Sandbox::User=root update && \
  apt-get -o APT::Sandbox::User=root -y upgrade && \
  rm -rf /var/lib/apt/lists/* && \
  npm pack npm@12.2.2 --pack-destination /tmp/npm-pin \
    --registry=https://artifactory.cloud.cms.gov/artifactory/api/npm/npm/ && \
  sha512sum -c /tmp/npm-pin/npm.sha512 && \
  npm install -g --ignore-scripts /tmp/npm-pin/npm-12.2.2.tgz && \
  rm -rf /tmp/npm-pin
USER 1000

WORKDIR /home/node/app

# The standalone server.js binds to $HOSTNAME. Kubernetes sets that to the pod
# name, which resolves to the pod IP only, so tests on localhost were refused.
ENV NODE_ENV=production \
  HOSTNAME=0.0.0.0 \
  PORT=3000
COPY --chown=node:node --from=builder /home/node/app/.next/standalone ./
COPY --chown=node:node --from=builder /home/node/app/.next/static ./.next/static

EXPOSE 3000

CMD ["node", "server.js"]
