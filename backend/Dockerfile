# syntax=docker/dockerfile:1

# ---------- étape 1 : build ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Wrapper + pom d'abord : cache des dépendances tant que le pom ne change pas
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q -DskipTests dependency:resolve-plugins dependency:resolve || true

# Code source, puis packaging
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests \
    && cp target/*.jar /app/app.jar

# ---------- étape 2 : runtime ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Utilisateur non-root + dossier de stockage des fichiers de cours
RUN groupadd -r educa && useradd -r -g educa educa \
    && mkdir -p /app/var/storage && chown -R educa:educa /app
USER educa

COPY --from=build --chown=educa:educa /app/app.jar /app/app.jar

EXPOSE 8081
ENV SPRING_PROFILES_ACTIVE=prod \
    STORAGE_LOCAL_PATH=/app/var/storage
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
