# Multi-stage Dockerfile for Vue.js demo application (SPA)
# Uses Artifactory mirrors per project policy with SHA256 digests

# Stage 1: Build
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/nodejs22@sha256:6efb3caa05792d177163f27ef1012c589777532ca94388f23141872fbef3882f AS builder

WORKDIR /home/node/app

# Copy dependency manifests
COPY --chown=node:node package*.json ./

# Install dependencies
RUN npm ci

# Copy application source
COPY . .

# Build Vue SPA (outputs to dist/)
RUN npm run build

# Stage 2: Runtime - Nginx to serve static files
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nginx/nginx@sha256:779b8bf0c36291971646465f5bca06a78514b4d976c5e54fc64dce70f6608001

WORKDIR /usr/share/nginx/html

# Copy built static files from builder (overwrites default nginx content)
COPY --from=builder /home/node/app/dist .

# Copy custom nginx configuration for SPA routing
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

# Nginx runs on port 3001 for this demo
EXPOSE 3001

CMD ["nginx", "-g", "daemon off;"]
