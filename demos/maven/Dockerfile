FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/maven/maven-openjdk-25@sha256:47a05928a05c7944f7ab7b4706ced8f6e30d685361e3daaf51a67cdcbf43b1d0 AS builder
# hadolint ignore=DL3002
USER 0
RUN dnf upgrade -y --releasever=latest --nodocs && dnf clean all && rm -rf /var/cache/yum
WORKDIR /build
COPY settings.xml ./
COPY pom.xml ./
COPY src ./src

RUN mvn --no-transfer-progress clean package -DskipTests -s settings.xml

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/maven/maven-openjdk-25@sha256:47a05928a05c7944f7ab7b4706ced8f6e30d685361e3daaf51a67cdcbf43b1d0

USER 0
RUN dnf upgrade -y --releasever=latest --nodocs && dnf clean all && rm -rf /var/cache/yum
USER 1001

WORKDIR /app
COPY --from=builder /build/target/site-1.0.0.jar ./app.jar
EXPOSE 3004

CMD ["java", "-jar", "app.jar"]
