# Temurin is installed from the Adoptium release tarballs rather than the
# eclipse-temurin images so the exact build can be pinned and checksum-verified.
# 25.0.5+8 is the first JDK 25 build with the eMudhra CA compromise fix
# (CVE-2026-91403 / CVE-2026-114820): https://adoptium.net/news/2026/10/temurin-october-2026-emudhra
FROM debian:trixie-slim AS temurin

ARG TEMURIN_VERSION=25.0.5+8
ARG JDK_SHA256_X64=68982529869257d0ee6b824ec5aa270ff8b118bb438670da0b97ebdab178d7fb
ARG JDK_SHA256_AARCH64=bb5b2ded252f21c6090cf780b0a408ad0d83bbb557e702955643557f13231880
ARG JRE_SHA256_X64=6fac98f0c8202efc3135a9e9e42aa61963ec42d1c5cbb40caf231e7b14a24ef6
ARG JRE_SHA256_AARCH64=963931723891302ebb2687178532c3d793d71e2577cd058061c7efb396140a86
ARG TARGETARCH
ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN set -eu; \
    case "${TARGETARCH:-$(dpkg --print-architecture)}" in \
      amd64) arch=x64;     jdk_sha="$JDK_SHA256_X64";     jre_sha="$JRE_SHA256_X64" ;; \
      arm64) arch=aarch64; jdk_sha="$JDK_SHA256_AARCH64"; jre_sha="$JRE_SHA256_AARCH64" ;; \
      *) echo "Unsupported architecture: $TARGETARCH" >&2; exit 1 ;; \
    esac; \
    tag="jdk-$(echo "$TEMURIN_VERSION" | sed 's/+/%2B/')"; \
    file_version="$(echo "$TEMURIN_VERSION" | tr + _)"; \
    base="https://github.com/adoptium/temurin25-binaries/releases/download/${tag}"; \
    for kind in jdk jre; do \
      eval "sha=\$${kind}_sha"; \
      curl -fsSLo /tmp/$kind.tar.gz "${base}/OpenJDK25U-${kind}_${arch}_linux_hotspot_${file_version}.tar.gz"; \
      echo "$sha  /tmp/$kind.tar.gz" | sha256sum -c -; \
      mkdir -p /opt/$kind; \
      tar -xzf /tmp/$kind.tar.gz -C /opt/$kind --strip-components=1; \
      rm /tmp/$kind.tar.gz; \
    done

# Fail the build if either compromised root is still in the bundled trust store
RUN for kind in jdk jre; do \
      /opt/$kind/bin/java -version; \
      if /opt/$kind/bin/keytool -list -v -cacerts -storepass changeit \
           | grep -Eq 'Owner: .*CN=(emSign ECC Root CA - G3|AAA Certificate Services)'; then \
        echo "Compromised eMudhra/AAA root present in $kind cacerts" >&2; exit 1; \
      fi; \
    done

FROM temurin AS build-env

ARG SBT_COMMIT=v2.0.12
ARG DEBIAN_FRONTEND=noninteractive
ENV JAVA_HOME=/opt/jdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

RUN apt-get update && apt-get install -y --no-install-recommends \
      curl ca-certificates gpg \
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

ENV JAVA_HOME=/opt/jdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

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

FROM debian:trixie-slim

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
ENV LANG=C.UTF-8
COPY --from=temurin /opt/jre ${JAVA_HOME}

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
      nginx wget libcap2-bin \
    && setcap 'cap_net_bind_service=+ep' /usr/sbin/nginx \
    && rm -rf /var/lib/apt/lists/* /etc/nginx/sites-enabled/default

RUN groupadd --system --gid 1001 app \
    && useradd  --system --uid 1001 --gid app --home /run --shell /usr/sbin/nologin app \
    && install -d -o app -g app /var/lib/nginx /var/log/nginx /var/run \
    && ln -sf /dev/stdout /var/log/nginx/access.log \
    && ln -sf /dev/stderr /var/log/nginx/error.log \
    && touch /var/run/nginx.pid && chown app:app /var/run/nginx.pid \
    && sed -i 's/^user www-data;/user app;/' /etc/nginx/nginx.conf

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
