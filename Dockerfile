# Single image for deployment: the Vue build is served by Spring Boot, so the page and the API share one origin.
# For local development with two containers, see docker-compose.yml.

FROM node:22-alpine AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend .
RUN npm run build

FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app
COPY backend/gradlew backend/settings.gradle.kts backend/build.gradle.kts ./
COPY backend/gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY backend/src src
COPY --from=frontend /app/dist src/main/resources/static
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre
RUN useradd --system --no-create-home app
WORKDIR /app
COPY --from=backend /app/build/libs/*-SNAPSHOT.jar app.jar
USER app
# Shares a server with other apps, so keep the heap small.
ENV JAVA_TOOL_OPTIONS="-Xmx256m -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
