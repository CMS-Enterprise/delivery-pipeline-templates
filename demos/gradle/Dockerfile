FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/gradle/gradle-jdk25@sha256:bd09bbd39ded1bd5584b0c4b3046207c4ac1f6992ba5adcbad4730d90f72e1fe AS builder
# hadolint ignore=DL3002
USER 0
WORKDIR /build

COPY build.gradle settings.gradle gradle.properties ./
COPY checkstyle.xml ./
COPY src ./src

RUN gradle --no-daemon --console=plain assemble -x test

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/redhat/openjdk/openjdk25-runtime-ubi9-slim@sha256:c1096b382fc79eb1ad17f1594a97279b63a376af3538d0adc14074ed2c8fe271
USER 0
RUN dnf -y upgrade && dnf clean all
USER 1000
WORKDIR /app
COPY --from=builder /build/build/libs/site-1.0.0.jar ./app.jar
EXPOSE 3006

CMD ["java", "-jar", "app.jar"]
