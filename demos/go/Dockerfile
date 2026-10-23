# Multi-stage Dockerfile for Go demo application
# Uses Artifactory mirrors per project policy with SHA256 digests

# Stage 1: Build
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/google/golang/ubi9/golang-1.26@sha256:f5a458e824d2044b2ba7ccf2b629ea9d45eb7417dded43cafcbccee914fa02f2 AS builder
USER 0

WORKDIR /build

# Copy go module files
COPY go.mod ./

# Copy application source
COPY . .

# Build static binary
# CGO_ENABLED=0 for fully static binary, works with scratch/alpine
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o go-site .

# Stage 2: Runtime - minimal Alpine image
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/alpinelinux/alpine@sha256:74b99970cdf759cf2dab93d40a369e2911eecf176fff14dea1257ec90f3de8f8

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
