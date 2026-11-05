FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/debian/nodejs@sha256:de5abd554ce2d5b736f0d85592f50d8648d80a7c0ab4d59ef45eeeb4edf6f66e AS builder
# hadolint ignore=DL3002
USER 0
RUN apt-get -o APT::Sandbox::User=root update && apt-get -o APT::Sandbox::User=root -y upgrade && rm -rf /var/lib/apt/lists/*
WORKDIR /home/node/app

COPY --chown=node:node package*.json .npmrc ./
RUN npm ci

COPY --chown=node:node . .
RUN npm run build

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/debian/nodejs@sha256:de5abd554ce2d5b736f0d85592f50d8648d80a7c0ab4d59ef45eeeb4edf6f66e
USER 0
RUN apt-get -o APT::Sandbox::User=root update && apt-get -o APT::Sandbox::User=root -y upgrade && rm -rf /var/lib/apt/lists/* && \
  npm install -g npm@12.2.2 --registry=https://artifactory.cloud.cms.gov/artifactory/api/npm/npm/
USER 1000

WORKDIR /home/node/app

ENV NODE_ENV=production
COPY --chown=node:node --from=builder /home/node/app/.next/standalone ./
COPY --chown=node:node --from=builder /home/node/app/.next/static ./.next/static

EXPOSE 3000

CMD ["node", "server.js"]
