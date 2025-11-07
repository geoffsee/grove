# Multi-stage build for Grove (Spring Boot + Kotlin)

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy Gradle wrapper and build files first for better layer caching
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x ./gradlew \
    && ./gradlew --no-daemon --version

# Copy sources and build the bootable jar
COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test


FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Copy the Spring Boot fat jar from the builder
COPY --from=build /workspace/build/libs/*.jar /app/app.jar

# Expose the application port
EXPOSE 8080

# Optional JVM flags via JAVA_OPTS
ENV JAVA_OPTS=""

# Default storage path is relative (storage/). Override via env if mounting /data.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

