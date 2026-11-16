# Contexte de build : la racine du dépôt (voir compose.yaml), pour que le cache Gradle
# ne soit invalidé que par backend/. Le frontend est déployé séparément sur Vercel (#5).

# ── Stage 1 : build du backend ───────────────────────────────────────────────
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Cache des dépendances Gradle (layer invalidé seulement si build.gradle change)
COPY backend/gradlew backend/gradlew.bat* ./
COPY backend/gradle ./gradle
RUN chmod +x gradlew
COPY backend/build.gradle backend/settings.gradle ./
RUN ./gradlew dependencies --no-daemon -q 2>/dev/null || true

# Compilation et packaging (sans tests — ils tournent en CI)
COPY backend/src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ── Stage 2 : runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
