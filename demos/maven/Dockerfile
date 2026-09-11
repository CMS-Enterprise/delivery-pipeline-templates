# Multi-stage Dockerfile for Maven/Spring Boot demo application
# Uses IronBank hardened images via Artifactory mirror

# Stage 1: Build
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/maven/maven-openjdk-17:3.9.16 AS builder

WORKDIR /app

# Copy Maven settings and project files
COPY settings.xml ./
COPY pom.xml ./
COPY src ./src

# Build the application (skip tests in container build)
RUN mvn clean package -DskipTests -s settings.xml

# Stage 2: Runtime - Java JRE only
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/eclipse-temurin/eclipse-temurin-17-jre:17

WORKDIR /app

# Copy the built JAR from builder
COPY --from=builder /app/target/site-1.0.0.jar ./app.jar

# Spring Boot runs on port 3004
EXPOSE 3004

# Run the Spring Boot application
CMD ["java", "-jar", "app.jar"]
