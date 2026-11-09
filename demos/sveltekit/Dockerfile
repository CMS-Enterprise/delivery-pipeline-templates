FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/debian/nodejs@sha256:d991b8e0a7a368047eb217d2a6214804a679b5a2c862334aace37360be0683f3 AS builder
# hadolint ignore=DL3002
WORKDIR /home/node/app

COPY --chown=node:node package*.json .npmrc ./
RUN npm ci

COPY --chown=node:node . .
RUN npm run build

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/debian/nodejs@sha256:d991b8e0a7a368047eb217d2a6214804a679b5a2c862334aace37360be0683f3
USER 0
RUN apt-get -o APT::Sandbox::User=root update && apt-get -o APT::Sandbox::User=root -y upgrade && rm -rf /var/lib/apt/lists/* && \
  npm install -g npm@12.2.2 --registry=https://artifactory.cloud.cms.gov/artifactory/api/npm/npm/
USER 1000

WORKDIR /home/node/app
ENV NODE_ENV=production

COPY --chown=node:node package*.json .npmrc ./
RUN npm ci --production
COPY --chown=node:node --from=builder /home/node/app/build ./build

EXPOSE 3002

USER 1000
CMD ["node", "build/index.js"]
