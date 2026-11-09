FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/debian/nodejs@sha256:d991b8e0a7a368047eb217d2a6214804a679b5a2c862334aace37360be0683f3 AS builder
# hadolint ignore=DL3002
WORKDIR /home/node/app

COPY --chown=node:node package*.json .npmrc ./
RUN npm ci

COPY --chown=node:node . .
RUN npm run build

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nginx/nginx@sha256:ea001ee95eee88d2228cbdbf65a30ae449a9ddd813adec8264302b225c462c5e
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
