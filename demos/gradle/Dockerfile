FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/gradle/gradle-jdk25@sha256:bd09bbd39ded1bd5584b0c4b3046207c4ac1f6992ba5adcbad4730d90f72e1fe AS builder
# hadolint ignore=DL3002
USER 0
WORKDIR /build

COPY build.gradle settings.gradle gradle.properties ./
COPY checkstyle.xml ./
COPY src ./src

RUN gradle --no-daemon --console=plain assemble -x test

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/redhat/openjdk/openjdk25-runtime-ubi9-slim@sha256:fbfeca1428773866d7e64d0d626b8bb4c774a9f4dcec2f69791ad27f936d6d7d
USER 0
RUN microdnf -y upgrade && microdnf clean all
USER 1000
WORKDIR /app
COPY --from=builder /build/build/libs/site-1.0.0.jar ./app.jar
EXPOSE 3006

CMD ["java", "-jar", "app.jar"]
