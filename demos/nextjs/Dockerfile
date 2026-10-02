# Multi-stage Dockerfile for Next.js demo application
# Uses Artifactory mirrors per project policy with SHA256 digests for reproducibility

# Stage 1: Build
# Node 18 Alpine from Artifactory Docker registry
FROM artifactory.cloud.cms.gov/docker/node@sha256:cacf10e99285cbbc891452e31249c1b5ec3ba225f40028fae946b75aeaf1b66a AS builder

WORKDIR /app

# Copy dependency manifests
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy application source
COPY . .

# Build Next.js application
# The standalone output includes all necessary dependencies
RUN npm run build

# Stage 2: Runtime
FROM artifactory.cloud.cms.gov/docker/node@sha256:cacf10e99285cbbc891452e31249c1b5ec3ba225f40028fae946b75aeaf1b66a

WORKDIR /app

# Set production environment
ENV NODE_ENV=production

# Copy standalone build output
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

# Next.js standalone server runs on port 3000
EXPOSE 3000

# Run the application
CMD ["node", "server.js"]
