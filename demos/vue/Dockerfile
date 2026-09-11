# Multi-stage Dockerfile for Vue.js demo application (SPA)
# Uses Artifactory mirrors per project policy with SHA256 digests

# Stage 1: Build
FROM artifactory.cloud.cms.gov/docker/node@sha256:980c5420a7a2ddcb44037726977f2a349e5c7b64217516c7488dce4c74d71583 AS builder

WORKDIR /app

# Copy dependency manifests
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy application source
COPY . .

# Build Vue SPA (outputs to dist/)
RUN npm run build

# Stage 2: Runtime - Nginx to serve static files
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nginx/nginx:1.28

WORKDIR /usr/share/nginx/html

# Remove default nginx static files
RUN rm -rf ./*

# Copy built static files from builder
COPY --from=builder /app/dist .

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
