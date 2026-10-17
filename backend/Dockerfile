# ── Stage 1 : build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Cache des dépendances Gradle (layer invalidé seulement si build.gradle change)
COPY gradlew gradlew.bat* ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon -q 2>/dev/null || true

# Compilation et packaging (sans tests — ils tournent en CI)
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ── Stage 2 : runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
