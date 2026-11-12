# Contexte de build : la racine du dépôt (voir compose.yaml), pour voir frontend/ et backend/.

# ── Stage 1 : build du frontend ──────────────────────────────────────────────
# Même version de Node que la CI (.github/workflows/ci.yml) : tests et bundle de production identiques
FROM node:24-alpine AS frontend
WORKDIR /app/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./

# Vite fige les VITE_* dans le bundle : les changer impose de reconstruire l'image
ARG VITE_ZITADEL_ISSUER
ARG VITE_ZITADEL_CLIENT_ID
ARG VITE_ZITADEL_PROJECT_ID
RUN npm run build

# ── Stage 2 : build du backend ───────────────────────────────────────────────
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
COPY --from=frontend /app/frontend/dist ./src/main/resources/static
RUN ./gradlew bootJar --no-daemon -x test

# ── Stage 3 : runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
