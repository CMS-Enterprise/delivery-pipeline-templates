# Multi-stage Dockerfile for SvelteKit demo application
# Uses Artifactory mirrors per project policy with SHA256 digests

# Stage 1: Build
FROM artifactory.cloud.cms.gov/docker/node@sha256:7b559cf7f77d837192392499a850abb1a3d1f49fbc2bd0692d54b212c9e38f6d AS builder

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
FROM artifactory.cloud.cms.gov/docker/node@sha256:7b559cf7f77d837192392499a850abb1a3d1f49fbc2bd0692d54b212c9e38f6d

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
