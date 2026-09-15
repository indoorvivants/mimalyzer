FROM eclipse-temurin:25 as build-env

RUN apt update && apt install -y curl
RUN curl -Lo /usr/bin/sbt https://raw.githubusercontent.com/sbt/sbt/refs/heads/develop/sbt && chmod +x /usr/bin/sbt


