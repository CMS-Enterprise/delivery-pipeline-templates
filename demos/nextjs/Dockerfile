FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/nodejs26@sha256:74237b1ccbe3f523fcd8b5a999a934595da60fe4f337ad9fa50303179ae16dc8 AS builder
# hadolint ignore=DL3002
USER 0
RUN apk update --no-cache && apk upgrade --no-cache
WORKDIR /home/node/app

COPY --chown=node:node package*.json ./
RUN npm ci

COPY --chown=node:node . .
RUN npm run build

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/nodejs26@sha256:74237b1ccbe3f523fcd8b5a999a934595da60fe4f337ad9fa50303179ae16dc8
USER 0
RUN apk update --no-cache && apk upgrade --no-cache && \
    cd /usr/lib/node_modules/npm && npm install brace-expansion@5.0.11 ip-address@10.3.1 tar@7.5.21 undici@6.29.1 --no-save
USER 1001

WORKDIR /home/node/app

ENV NODE_ENV=production
COPY --chown=node:node --from=builder /home/node/app/.next/standalone ./
COPY --chown=node:node --from=builder /home/node/app/.next/static ./.next/static

EXPOSE 3000

CMD ["node", "server.js"]
