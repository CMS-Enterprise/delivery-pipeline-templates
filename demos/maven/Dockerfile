# Multi-stage Dockerfile for Maven/Spring Boot demo application
# Uses IronBank hardened images via Artifactory mirror

# Stage 1: Build
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/maven/maven-openjdk-17@sha256:6ffd5544a5faacf3b31913ddb8369f1190d4b9376aa77f15b4c2838527d11d36 AS builder
USER 0

WORKDIR /build

# Copy Maven settings and project files
COPY settings.xml ./
COPY pom.xml ./
COPY src ./src

# Build the application (skip tests in container build)
RUN mvn clean package -DskipTests -s settings.xml

# Stage 2: Runtime - Java JRE only
FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/maven/maven-openjdk-25@sha256:9ab7321cc5f4034c0a039c15bfdbe22be1ac1c7eebef8dda5c8d3040d8fe5402

WORKDIR /app

# Copy the built JAR from builder
COPY --from=builder /build/target/site-1.0.0.jar ./app.jar

# Spring Boot runs on port 3004
EXPOSE 3004

# Run the Spring Boot application
CMD ["java", "-jar", "app.jar"]
