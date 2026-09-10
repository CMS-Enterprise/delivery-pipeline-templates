# Multi-stage Dockerfile for Maven/Spring Boot demo application
# Uses Artifactory mirrors per project policy with SHA256 digests

# Stage 1: Build
FROM artifactory.cloud.cms.gov/docker/maven@sha256:880934ae394bf91bc3e57d573e4fc04774f064f3c4df7ccd7cc10b3b126737bf AS builder

WORKDIR /app

# Copy Maven project files
COPY pom.xml ./
COPY src ./src

# Build the application (skip tests in container build)
RUN mvn clean package -DskipTests

# Stage 2: Runtime - Java JRE only
FROM artifactory.cloud.cms.gov/docker/eclipse-temurin@sha256:27cc0849148c0fd32ee8e95988917becf9bc96a3182a24f99d9763aa8e90f8cb

WORKDIR /app

# Copy the built JAR from builder
COPY --from=builder /app/target/site-1.0.0.jar ./app.jar

# Spring Boot runs on port 3004
EXPOSE 3004

# Run the Spring Boot application
CMD ["java", "-jar", "app.jar"]
