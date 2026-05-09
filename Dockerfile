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

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]