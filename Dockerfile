FROM gradle:jdk17 AS builder
WORKDIR /app

ENV GRADLE_USER_HOME /cache
COPY build.gradle.kts settings.gradle.kts ./
COPY src/main/ src/main/
RUN gradle installDist

FROM eclipse-temurin:17
WORKDIR /app
COPY --from=builder /app/build/install/RuneBotKt .

CMD ["/app/bin/RuneBotKt"]