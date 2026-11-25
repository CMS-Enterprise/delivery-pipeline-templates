FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/maven/maven-openjdk-25@sha256:15c31858a43d176f412d02bdb6434f00707038afb7ed75b72d4c06df304338a7 AS builder
# hadolint ignore=DL3002
USER 0
# OS_PATCH_DATE changes daily (jfrogBuildPublish sets it), so a layer cache
# never serves an OS upgrade older than a day.
ARG OS_PATCH_DATE
RUN echo "OS packages as of ${OS_PATCH_DATE:-unset}" && \
  dnf upgrade -y --releasever=latest --nodocs && dnf clean all && rm -rf /var/cache/yum
WORKDIR /build
COPY settings.xml ./
COPY pom.xml ./
COPY src ./src

RUN mvn --no-transfer-progress clean package -DskipTests -s settings.xml

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/redhat/openjdk/openjdk25-runtime-ubi9-slim@sha256:b4b80a506a46250161592dca3f75fb6609da0aef5a52ea38c5109b905e96e831
USER 0
# OS_PATCH_DATE changes daily (jfrogBuildPublish sets it), so a layer cache
# never serves an OS upgrade older than a day.
ARG OS_PATCH_DATE
RUN echo "OS packages as of ${OS_PATCH_DATE:-unset}" && \
  microdnf -y upgrade && microdnf clean all
USER 1000
WORKDIR /app
COPY --from=builder /build/target/site-1.0.0.jar ./app.jar
EXPOSE 3004

CMD ["java", "-jar", "app.jar"]
