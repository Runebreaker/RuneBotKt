# syntax=docker/dockerfile:1

FROM gradle:8.14-jdk21 AS build
WORKDIR /home/gradle/RuneBotKt

COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN gradle --no-daemon installDist

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /home/gradle/RuneBotKt/build/install/RuneBotKt/ ./

ENTRYPOINT ["/app/bin/RuneBotKt"]
