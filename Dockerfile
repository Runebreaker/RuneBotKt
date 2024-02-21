FROM gradle:8.6.0-jdk21 AS builder
WORKDIR /app

ENV GRADLE_USER_HOME /cache
COPY build.gradle.kts settings.gradle.kts ./
COPY src/main/ src/main/
RUN gradle installDist

FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=builder /app/build/install/RuneBotKt .

CMD ["/app/bin/RuneBotKt"]