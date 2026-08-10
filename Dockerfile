# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Image default; override with `docker run -e SPRING_PROFILES_ACTIVE=local` for local container testing
ENV SPRING_PROFILES_ACTIVE=prod

# Render injects PORT; server.port already resolves it via ${PORT:8080}
# Cap the heap well inside Render's free-tier 512MB container limit to avoid an OOM-kill
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-jar", "app.jar"]
