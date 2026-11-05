FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/nodejs26@sha256:e15065bd6c719aaa8985190779e633f6479b1ac4cc51427f5f7d55662a3ac06e AS builder
# hadolint ignore=DL3002
USER 0
RUN apk update --no-cache && apk upgrade --no-cache
WORKDIR /home/node/app

COPY --chown=node:node package*.json .npmrc ./
RUN npm ci

COPY --chown=node:node . .
RUN npm run build

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/nodejs26@sha256:e15065bd6c719aaa8985190779e633f6479b1ac4cc51427f5f7d55662a3ac06e
USER 0
RUN apk update --no-cache && apk upgrade --no-cache && \
  npm install -g npm@12.2.2 --registry=https://artifactory.cloud.cms.gov/artifactory/api/npm/npm/
USER 1001

WORKDIR /home/node/app

ENV NODE_ENV=production
COPY --chown=node:node --from=builder /home/node/app/.next/standalone ./
COPY --chown=node:node --from=builder /home/node/app/.next/static ./.next/static

EXPOSE 3000

CMD ["node", "server.js"]
