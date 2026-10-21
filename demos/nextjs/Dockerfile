# Multi-stage Dockerfile for Next.js demo application
# Uses Artifactory mirrors per project policy with SHA256 digests for reproducibility

# Stage 1: Build
# Node 18 Alpine from Artifactory Docker registry
FROM artifactory.cloud.cms.gov/docker/node@sha256:7b559cf7f77d837192392499a850abb1a3d1f49fbc2bd0692d54b212c9e38f6d AS builder

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
FROM artifactory.cloud.cms.gov/docker/node@sha256:7b559cf7f77d837192392499a850abb1a3d1f49fbc2bd0692d54b212c9e38f6d

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
