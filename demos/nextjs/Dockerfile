# Multi-stage Dockerfile for Next.js demo application
# Uses Artifactory mirrors per project policy with SHA256 digests for reproducibility

# Stage 1: Build
# Node 18 Alpine from Artifactory Docker registry
FROM artifactory.cloud.cms.gov/docker/node@sha256:980c5420a7a2ddcb44037726977f2a349e5c7b64217516c7488dce4c74d71583 AS builder

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
FROM artifactory.cloud.cms.gov/docker/node@sha256:980c5420a7a2ddcb44037726977f2a349e5c7b64217516c7488dce4c74d71583

WORKDIR /app

# Set production environment
ENV NODE_ENV=production

# Copy standalone build output
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/public ./public

# Next.js standalone server runs on port 3000
EXPOSE 3000

# Run the application
CMD ["node", "server.js"]
