FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/maven/maven-openjdk-25@sha256:47a05928a05c7944f7ab7b4706ced8f6e30d685361e3daaf51a67cdcbf43b1d0 AS builder
USER 0

WORKDIR /build
COPY settings.xml ./
COPY pom.xml ./
COPY src ./src

RUN mvn --no-transfer-progress clean package -DskipTests -s settings.xml

FROM artifactory.cloud.cms.gov/gi-gantuar-ironbank/ironbank/opensource/maven/maven-openjdk-25@sha256:47a05928a05c7944f7ab7b4706ced8f6e30d685361e3daaf51a67cdcbf43b1d0

USER 0
RUN dnf upgrade -y --nodocs && dnf clean all || microdnf update -y && microdnf clean all || true
USER 1001

WORKDIR /app
COPY --from=builder /build/target/site-1.0.0.jar ./app.jar
EXPOSE 3004

USER 1001
CMD ["java", "-jar", "app.jar"]
