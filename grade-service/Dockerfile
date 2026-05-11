# Стадія збірки
FROM gradle:8.5-jdk21-alpine AS build
WORKDIR /app

# Копіюємо gradle wrapper та конфігурацію
COPY gradle gradle
COPY gradlew .
RUN chmod +x gradlew
COPY build.gradle .
COPY settings.gradle .

# Завантажуємо залежності
RUN ./gradlew dependencies --no-daemon || return 0

# Копіюємо весь код проєкту
COPY src src

# Збираємо додаток БЕЗ ТЕСТІВ
RUN ./gradlew clean bootJar -x test --no-daemon && \
    echo "=== Built JAR files ===" && \
    ls -lh /app/build/libs/

# Стадія виконання
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Створюємо користувача
RUN addgroup -S spring && adduser -S spring -G spring && \
    mkdir -p /logs && \
    chown -R spring:spring /logs

USER spring:spring

# Копіюємо JAR
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080 5005

ENTRYPOINT ["sh", "-c", "java ${JAVA_DEBUG_OPTS} -jar app.jar"]