# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY pom.xml ./
COPY forum-common ./forum-common
COPY forum-pojo ./forum-pojo
COPY forum-server ./forum-server

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B --no-transfer-progress -DskipTests package
RUN cp forum-server/target/forum-server-*.jar /build/app.jar

FROM eclipse-temurin:17-jre

WORKDIR /app
ENV TZ=Asia/Shanghai

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 --create-home lifeqa \
    && chown -R lifeqa:lifeqa /app

USER lifeqa
COPY --from=builder --chown=lifeqa:lifeqa /build/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
