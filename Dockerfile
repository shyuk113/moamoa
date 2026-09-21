# Build stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
COPY gradle/wrapper gradle/wrapper
RUN chmod +x gradlew && ./gradlew bootJar

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENV PORT=8080
ENV SCHEDULING_ENABLED=true
ENV SECURE_COOKIE=true

ENTRYPOINT ["java", "-Dserver.port=${PORT:-8080}", "-jar", "app.jar"]