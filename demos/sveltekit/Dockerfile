# Multi-stage Dockerfile for SvelteKit demo application
# Uses Artifactory mirrors per project policy with SHA256 digests

# Stage 1: Build
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/nodejs22@sha256:66e71a96660a4dacab348bda3707885f6790cb731a9fe63cabd857d6e4b1a7de AS builder

WORKDIR /home/node/app

# Copy dependency manifests
COPY --chown=node:node package*.json ./

# Install dependencies
RUN npm ci

# Copy application source
COPY . .

# Build SvelteKit application (adapter-node outputs to build/)
RUN npm run build

# Stage 2: Runtime
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/nodejs22@sha256:66e71a96660a4dacab348bda3707885f6790cb731a9fe63cabd857d6e4b1a7de

WORKDIR /home/node/app

# Set production environment
ENV NODE_ENV=production

# Copy package manifests
COPY --chown=node:node package*.json ./

# Install production dependencies only
RUN npm ci --production

# Copy built application from builder
COPY --chown=node:node --from=builder /home/node/app/build ./build

# SvelteKit server runs on port 3002
EXPOSE 3002

# Run the SvelteKit node server
CMD ["node", "build/index.js"]
