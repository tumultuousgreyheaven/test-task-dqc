# syntax=docker/dockerfile:1

# ---------- сборка ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# сначала только pom.xml: слой с зависимостями кэшируется, пока pom не менялся
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:go-offline

COPY src ./src
# тесты запускаются отдельно (mvn test); при сборке образа они не нужны
RUN --mount=type=cache,target=/root/.m2 mvn -B package -DskipTests

# ---------- запуск ----------
FROM eclipse-temurin:17-jre
# curl нужен для healthcheck в docker-compose.yml
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --no-create-home app

WORKDIR /app
COPY --from=build /workspace/target/dqc.jar app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
