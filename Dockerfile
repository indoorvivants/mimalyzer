FROM eclipse-temurin:25-jdk-noble AS build-env

ARG SBT_COMMIT=v2.0.9
ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
      curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*
RUN curl -fsSLo /usr/bin/sbt "https://raw.githubusercontent.com/sbt/sbt/${SBT_COMMIT}/sbt" \
    && chmod +x /usr/bin/sbt
WORKDIR /workdir
COPY project/ project/
COPY build.sbt .
RUN sbt update

FROM build-env AS backend-build
WORKDIR /workdir
COPY . .
RUN sbt bundleBackend

FROM build-env AS frontend-build
WORKDIR /workdir
RUN install -d -m 0755 /etc/apt/keyrings \
    && curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key \
      | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg \
    && echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_22.x nodistro main" \
      > /etc/apt/sources.list.d/nodesource.list \
    && apt-get update && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

COPY . .
RUN sbt fullLinkJS && cd frontend && npm ci && npm run build

FROM nginx:1.27-bookworm

ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates gpg wget libcap2-bin make \
    && install -d -m 0755 /etc/apt/keyrings \
    && wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public \
      | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg \
    && echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" \
      > /etc/apt/sources.list.d/adoptium.list \
    && apt-get update && apt-get install -y --no-install-recommends temurin-24-jre \
    && setcap 'cap_net_bind_service=+ep' /usr/sbin/nginx \
    && apt-get purge -y --auto-remove gpg wget libcap2-bin \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system --gid 1001 app \
    && useradd  --system --uid 1001 --gid app --home /run --shell /usr/sbin/nologin app \
    && install -d -o app -g app /var/cache/nginx /var/run \
    && touch /var/run/nginx.pid && chown app:app /var/run/nginx.pid \
    && sed -i 's/^user  nginx;/user  app;/' /etc/nginx/nginx.conf

COPY --from=backend-build  --chown=app:app /workdir/build/app     /run/app
COPY --from=backend-build  --chown=app:app /workdir/build/.libs   /run/.libs
COPY --from=frontend-build --chown=app:app /workdir/frontend/dist /run/frontend

COPY ./nginx/nginx.conf    /etc/nginx/conf.d/default.conf
COPY ./nginx/entrypoint.sh /run/entrypoint.sh
RUN chmod 0555 /run/entrypoint.sh

USER app:app
WORKDIR /run
EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget -qO- http://127.0.0.1:80/ >/dev/null || exit 1

STOPSIGNAL SIGTERM
CMD ["/run/entrypoint.sh"]
