# Multi-stage Dockerfile for Go demo application
# Uses Artifactory mirrors per project policy with SHA256 digests

# Stage 1: Build
FROM artifactory.cloud.cms.gov/docker/golang:latest AS builder

WORKDIR /app

# Copy go module files
COPY go.mod go.sum ./

# Download dependencies
RUN go mod download

# Copy application source
COPY . .

# Build static binary
# CGO_ENABLED=0 for fully static binary, works with scratch/alpine
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o go-site .

# Stage 2: Runtime - minimal Alpine image
FROM artifactory.cloud.cms.gov/docker/alpine@sha256:21dc6063fd678b478f57c0e13f47560d0ea4eeba26dfc947b2a4f81f686b9f45

WORKDIR /app

# Copy binary from builder
COPY --from=builder /app/go-site ./

# Copy static assets and templates
COPY --from=builder /app/static ./static
COPY --from=builder /app/templates ./templates

# Go application runs on port 3005
EXPOSE 3005

# Run the binary
CMD ["./go-site"]
