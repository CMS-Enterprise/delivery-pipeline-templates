# Multi-stage Dockerfile for Next.js demo application
# Uses Artifactory mirrors per project policy with SHA256 digests for reproducibility

# Stage 1: Build
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/nodejs22@sha256:66e71a96660a4dacab348bda3707885f6790cb731a9fe63cabd857d6e4b1a7de AS builder

WORKDIR /home/node/app

# Copy dependency manifests
COPY --chown=node:node package*.json ./

# Install dependencies
RUN npm ci

# Copy application source
COPY . .

# Build Next.js application
# The standalone output includes all necessary dependencies
RUN npm run build

# Stage 2: Runtime
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/nodejs/nodejs22@sha256:66e71a96660a4dacab348bda3707885f6790cb731a9fe63cabd857d6e4b1a7de

WORKDIR /home/node/app

# Set production environment
ENV NODE_ENV=production

# Copy standalone build output
COPY --chown=node:node --from=builder /home/node/app/.next/standalone ./
COPY --chown=node:node --from=builder /home/node/app/.next/static ./.next/static

# Next.js standalone server runs on port 3000
EXPOSE 3000

# Run the application
CMD ["node", "server.js"]
