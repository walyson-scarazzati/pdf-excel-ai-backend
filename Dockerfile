ARG APP_VERSION=0.0.1-SNAPSHOT

FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG APP_VERSION
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -Drevision=${APP_VERSION} dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests -Drevision=${APP_VERSION}

FROM eclipse-temurin:21-jre
ARG APP_VERSION
WORKDIR /app
LABEL org.opencontainers.image.version=$APP_VERSION
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=85 -XX:InitialRAMPercentage=10 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-por \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
