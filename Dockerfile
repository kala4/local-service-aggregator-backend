# Step 1: Build JAR via Gradle
FROM gradle:8.5-jdk21-alpine AS builder
WORKDIR /app

# Копируем обвязку Gradle и файлы конфигурации (включая .kts, если используются)
COPY gradle/ ./gradle/
COPY gradlew build.gradle* settings.gradle* ./

# Прекачиваем зависимости в отдельный кэш-слой
RUN ./gradlew dependencies --no-daemon || true

# Копируем исходный код и собираем исполняемый JAR
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# Step 2: Runtime Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/build/libs/*.jar app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]