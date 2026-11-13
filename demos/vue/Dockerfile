FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/debian/nodejs@sha256:22dbe44d2d557968cc06e491be2546f9dd50ccdc923bc03e97f6862bcc1bbaa0 AS builder
# hadolint ignore=DL3002
WORKDIR /home/node/app

COPY --chown=node:node package*.json .npmrc ./
RUN npm ci

COPY --chown=node:node . .
RUN npm run build

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nginx/nginx@sha256:c406814804624c8aa0dffed6cb8f9843d87fa09e83fdfa328f55f461e1a22561
USER 0
RUN dnf upgrade -y --nodocs && dnf clean all && rm -rf /var/cache/dnf

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
