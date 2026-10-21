# Multi-stage Dockerfile for Go demo application
# Uses Artifactory mirrors per project policy with SHA256 digests

# Stage 1: Build
FROM artifactory.cloud.cms.gov/docker/golang@sha256:838da1d688161cd81dee9f6dcddc85fed0686a7bec6e9c0998a20d78bc4e394b AS builder

WORKDIR /build

# Copy go module files
COPY go.mod ./

# Copy application source
COPY . .

# Build static binary
# CGO_ENABLED=0 for fully static binary, works with scratch/alpine
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o go-site .

# Stage 2: Runtime - minimal Alpine image
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/alpinelinux/alpine@sha256:6c35103363264d6ac16c01f518ed169d91424c79f160a9c922c05de094ced525

WORKDIR /app

# Copy binary from builder
COPY --from=builder /build/go-site ./

# Copy static assets and templates
COPY --from=builder /build/static ./static
COPY --from=builder /build/templates ./templates

# Go application runs on port 3005
EXPOSE 3005

# Run the binary
CMD ["./go-site"]
