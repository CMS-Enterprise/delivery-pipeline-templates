# Multi-stage Dockerfile for SvelteKit demo application
# Uses Artifactory mirrors per project policy with SHA256 digests

# Stage 1: Build
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/nodejs26@sha256:74237b1ccbe3f523fcd8b5a999a934595da60fe4f337ad9fa50303179ae16dc8 AS builder

WORKDIR /home/node/app

# Copy dependency manifests
COPY --chown=node:node package*.json ./

# Install dependencies
RUN npm ci

# Copy application source
COPY --chown=node:node . .

# Build SvelteKit application (adapter-node outputs to build/)
RUN npm run build

# Stage 2: Runtime
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/nodejs26@sha256:74237b1ccbe3f523fcd8b5a999a934595da60fe4f337ad9fa50303179ae16dc8

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
USER 1001
CMD ["node", "build/index.js"]
