# Multi-stage Dockerfile for Maven/Spring Boot demo application
# Uses IronBank hardened images via Artifactory mirror

# Stage 1: Build
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/maven/maven-openjdk-25@sha256:47a05928a05c7944f7ab7b4706ced8f6e30d685361e3daaf51a67cdcbf43b1d0 AS builder
USER 0

WORKDIR /build

# Copy Maven settings and project files
COPY settings.xml ./
COPY pom.xml ./
COPY src ./src

# Build the application (skip tests in container build)
RUN mvn clean package -DskipTests -s settings.xml

# Stage 2: Runtime - Java JRE only
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/maven/maven-openjdk-25@sha256:47a05928a05c7944f7ab7b4706ced8f6e30d685361e3daaf51a67cdcbf43b1d0

WORKDIR /app

# Copy the built JAR from builder
COPY --from=builder /build/target/site-1.0.0.jar ./app.jar

# Spring Boot runs on port 3004
EXPOSE 3004

# Run the Spring Boot application
CMD ["java", "-jar", "app.jar"]
