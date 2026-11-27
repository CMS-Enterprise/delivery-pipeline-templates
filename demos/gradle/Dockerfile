FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/gradle/gradle-jdk25@sha256:e2dbbe8b852f47be9632d47942da0611083850f53efe417b3088e0369bb50e37 AS builder
# hadolint ignore=DL3002
USER 0
WORKDIR /build

COPY build.gradle settings.gradle gradle.properties gradle.lockfile ./
COPY checkstyle.xml ./
COPY src ./src

RUN gradle --no-daemon --console=plain assemble -x test

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/redhat/openjdk/openjdk25-runtime-ubi9-slim@sha256:b4b80a506a46250161592dca3f75fb6609da0aef5a52ea38c5109b905e96e831
USER 0
# OS_PATCH_DATE changes daily (jfrogBuildPublish sets it), so a layer cache
# never serves an OS upgrade older than a day.
ARG OS_PATCH_DATE
RUN echo "OS packages as of ${OS_PATCH_DATE:-unset}" && \
  microdnf -y upgrade && microdnf clean all
USER 1000
WORKDIR /app
COPY --from=builder /build/build/libs/site-1.0.0.jar ./app.jar
EXPOSE 3006

CMD ["java", "-jar", "app.jar"]
