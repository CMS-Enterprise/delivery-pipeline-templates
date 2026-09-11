# Multi-stage Dockerfile for Maven/Spring Boot demo application
# Uses IronBank hardened images via Artifactory mirror

# Stage 1: Build
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/maven/maven-openjdk-17@sha256:fc46e3ef7fb1c118dc4f13e8758069a5afe5134d29e138d3b4be8fb159be832a AS builder

WORKDIR /app

# Copy Maven settings and project files
COPY settings.xml ./
COPY pom.xml ./
COPY src ./src

# Build the application (skip tests in container build)
RUN mvn clean package -DskipTests -s settings.xml

# Stage 2: Runtime - Java JRE only
FROM artifactory.cloud.cms.gov/docker/eclipse-temurin@sha256:27cc0849148c0fd32ee8e95988917becf9bc96a3182a24f99d9763aa8e90f8cb

WORKDIR /app

# Copy the built JAR from builder
COPY --from=builder /app/target/site-1.0.0.jar ./app.jar

# Spring Boot runs on port 3004
EXPOSE 3004

# Run the Spring Boot application
CMD ["java", "-jar", "app.jar"]
