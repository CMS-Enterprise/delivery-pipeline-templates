FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/debian/nodejs@sha256:90b09acffe6c620be153311f11d50d72427ab08581de2c7344de9ec7d820a9dc AS builder
# hadolint ignore=DL3002
WORKDIR /home/node/app

COPY --chown=node:node package*.json .npmrc ./
RUN npm ci --ignore-scripts

COPY --chown=node:node . .
RUN npm run build

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nginx/nginx@sha256:e853b25bb2d472fd81e4d31c16fd2666588e25874ce35efd56079fc56c2b3d23
USER 0
# OS_PATCH_DATE changes daily (jfrogBuildPublish sets it), so a layer cache
# never serves an OS upgrade older than a day.
ARG OS_PATCH_DATE
RUN echo "OS packages as of ${OS_PATCH_DATE:-unset}" && \
  dnf upgrade -y --nodocs && dnf clean all && rm -rf /var/cache/dnf

WORKDIR /usr/share/nginx/html
COPY --from=builder /home/node/app/dist .
COPY <<'EOF' /etc/nginx/conf.d/default.conf
server {
    listen 3001;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    # SPA fallback routing - all routes serve index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
EOF

EXPOSE 3001

USER 101
CMD ["nginx", "-g", "daemon off;"]
