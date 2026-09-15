FROM eclipse-temurin:25 as build-env

RUN apt update && apt install -y curl
RUN curl -Lo /usr/bin/sbt https://raw.githubusercontent.com/sbt/sbt/refs/heads/develop/sbt && chmod +x /usr/bin/sbt
WORKDIR /workdir
COPY project/ project/
COPY build.sbt .
RUN sbt update

FROM build-env as backend-build
WORKDIR /workdir
COPY . .
RUN sbt bundleBackend

FROM build-env as frontend-build
WORKDIR /workdir
RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
   && apt-get install -y nodejs \
   && rm -rf /var/lib/apt/lists/*

COPY . .
RUN sbt fullLinkJS && cd frontend && npm i && npm run build

FROM nginx
RUN apt update && apt install -y gpg wget && \
    wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null && \
    echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list && \
    apt update && apt install -y temurin-24-jdk make

COPY --from=backend-build /workdir/build/app /run/app
COPY --from=backend-build /workdir/build/.libs /run/.libs
COPY --from=frontend-build /workdir/frontend/dist /run/frontend

COPY ./nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY ./nginx/entrypoint.sh /run/entrypoint.sh

EXPOSE 80

WORKDIR /run
CMD ["/run/entrypoint.sh"]
