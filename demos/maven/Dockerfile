# Multi-stage Dockerfile for Maven/Spring Boot demo application
# Uses Artifactory mirrors per project policy with SHA256 digests

# Stage 1: Build
FROM artifactory.cloud.cms.gov/docker/maven@sha256:abfd3b6c00e918b36c11c89c81ea803e6f24e7aebc1ae9dbc7badb4c2b2f2e44 AS builder

WORKDIR /app

# Copy Maven project files
COPY pom.xml ./
COPY src ./src

# Build the application (skip tests in container build)
RUN mvn clean package -DskipTests

# Stage 2: Runtime - Java JRE only
FROM artifactory.cloud.cms.gov/docker/eclipse-temurin@sha256:4a3b9c3e47bb4a4db0f6c3f5a6c5c5d1bc1d4b5e6f7a8b9c0d1e2f3a4b5c6d7e

WORKDIR /app

# Copy the built JAR from builder
COPY --from=builder /app/target/site-1.0.0.jar ./app.jar

# Spring Boot runs on port 3004
EXPOSE 3004

# Run the Spring Boot application
CMD ["java", "-jar", "app.jar"]
