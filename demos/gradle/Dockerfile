FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/gradle/gradle-jdk25@sha256:bd09bbd39ded1bd5584b0c4b3046207c4ac1f6992ba5adcbad4730d90f72e1fe AS builder
# hadolint ignore=DL3002
USER 0
WORKDIR /build

COPY build.gradle settings.gradle gradle.properties ./
COPY checkstyle.xml ./
COPY src ./src

RUN gradle --no-daemon --console=plain assemble -x test

FROM artifactory.cloud.cms.gov/docker/eclipse-temurin@sha256:56f273059de6b13a6b49fe0ded691ed2d0399bbee76156a8f2d385f7524a3d43

WORKDIR /app
COPY --from=builder /build/build/libs/site-1.0.0.jar ./app.jar
EXPOSE 3006

USER 1001
CMD ["java", "-jar", "app.jar"]
