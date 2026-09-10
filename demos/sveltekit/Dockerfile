# Multi-stage Dockerfile for SvelteKit demo application
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

# Build SvelteKit application (adapter-node outputs to build/)
RUN npm run build

# Stage 2: Runtime
FROM artifactory.cloud.cms.gov/docker/node@sha256:980c5420a7a2ddcb44037726977f2a349e5c7b64217516c7488dce4c74d71583

WORKDIR /app

# Set production environment
ENV NODE_ENV=production

# Copy package manifests
COPY package*.json ./

# Install production dependencies only
RUN npm ci --production

# Copy built application from builder
COPY --from=builder /app/build ./build

# SvelteKit server runs on port 3002
EXPOSE 3002

# Run the SvelteKit node server
CMD ["node", "build/index.js"]
