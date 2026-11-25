FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/debian/nodejs@sha256:22dbe44d2d557968cc06e491be2546f9dd50ccdc923bc03e97f6862bcc1bbaa0 AS builder
# hadolint ignore=DL3002
WORKDIR /home/node/app

COPY --chown=node:node package*.json .npmrc ./
RUN npm ci --ignore-scripts

COPY --chown=node:node . .
RUN npm run build

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/debian/nodejs@sha256:22dbe44d2d557968cc06e491be2546f9dd50ccdc923bc03e97f6862bcc1bbaa0
USER 0
# npm is installed from a tarball checked against the static sha512 in
# npm.sha512 (updated by scripts/update-pins.sh), not trusted by version alone.
COPY npm.sha512 /tmp/npm-pin/
RUN apt-get -o APT::Sandbox::User=root update && \
  apt-get -o APT::Sandbox::User=root -y upgrade && \
  rm -rf /var/lib/apt/lists/* && \
  cd /tmp/npm-pin && \
  npm pack npm@12.2.2 \
    --registry=https://artifactory.cloud.cms.gov/artifactory/api/npm/npm/ && \
  sha512sum -c npm.sha512 && \
  npm install -g --ignore-scripts ./npm-12.2.2.tgz && \
  cd / && rm -rf /tmp/npm-pin
USER 1000

WORKDIR /home/node/app
ENV NODE_ENV=production

COPY --chown=node:node package*.json .npmrc ./
RUN npm ci --omit=dev --ignore-scripts
COPY --chown=node:node --from=builder /home/node/app/build ./build

EXPOSE 3002

USER 1000
CMD ["node", "build/index.js"]
