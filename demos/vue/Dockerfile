FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/nodejs26@sha256:74237b1ccbe3f523fcd8b5a999a934595da60fe4f337ad9fa50303179ae16dc8 AS builder
# hadolint ignore=DL3002
WORKDIR /home/node/app

COPY --chown=node:node package*.json ./
RUN npm ci

COPY --chown=node:node . .
RUN npm run build

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nginx/nginx@sha256:add74d903c9a691882e5732c0e78f8abdc50b493cd39f5ddffc29132f50d65da

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
