# Build scala-cli container

FROM node:22 as scala-cli-build

WORKDIR /usr/local/bin

RUN wget https://raw.githubusercontent.com/VirtusLab/scala-cli/main/scala-cli.sh && \
    mv scala-cli.sh scala-cli && \
    chmod +x scala-cli && \
    scala-cli config power true && \
    scala-cli version && \
    echo '@main def hello = println(42)' | scala-cli run _ --js -S 3.5.2

# Build frontend

FROM scala-cli-build as frontend-build

WORKDIR /source
COPY shared shared

WORKDIR /source/frontend
COPY frontend/ .
RUN npm install && npm run build

# Build backend

FROM scala-cli-build as backend-build

WORKDIR /source
COPY shared shared
COPY compiler-interface compiler-interface

WORKDIR /source/backend
COPY backend/ .
RUN scala-cli package . --assembly -f -o ./backend-assembly

# Build scala213 compiler bridge

FROM scala-cli-build as scala-213-bridge-build

WORKDIR /source/compiler-interface
COPY compiler-interface/ .

WORKDIR /source/scala-213-bridge
COPY scala-213-bridge/ .
RUN 
RUN scala-cli package . --library -f -o ./scala213bridge.jar

# Build scala212 compiler bridge

FROM scala-cli-build as scala-212-bridge-build

WORKDIR /source/compiler-interface
COPY compiler-interface/ .

WORKDIR /source/scala-212-bridge
COPY scala-212-bridge/ .
RUN 
RUN scala-cli package . --library -f -o ./scala213bridge.jar


# Build scala3 compiler bridge

FROM scala-cli-build as scala-3-bridge-build

WORKDIR /source/compiler-interface
COPY compiler-interface/ .

WORKDIR /source/scala-3-bridge
COPY scala-3-bridge/ .
RUN 
RUN scala-cli package . --library -f -o ./scala3bridge.jar


# Final container build

FROM nginx

RUN apt update && apt install -y gpg wget && \
    wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null && \
    echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list && \
    apt update && apt install -y temurin-23-jdk

RUN curl -fLo coursier https://github.com/coursier/launchers/raw/master/coursier && \
    chmod +x coursier && \
    mv ./coursier /usr/local/bin/cs && \
    cs --help

ENV SCALA_213_CLASSPATH_FILE /app/scala213.classpath 
RUN mkdir -p /app && cs fetch -p org.scala-lang:scala-compiler:2.13.15 > /app/scala213.classpath

ENV SCALA_212_CLASSPATH_FILE /app/scala212.classpath 
RUN mkdir -p /app && cs fetch -p org.scala-lang:scala-compiler:2.12.20 > /app/scala212.classpath

ENV SCALA_3_CLASSPATH_FILE /app/scala3.classpath 
RUN mkdir -p /app && cs fetch -p org.scala-lang:scala3-compiler_3:3.3.4 > /app/scala3.classpath

COPY ./nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY ./nginx/entrypoint.sh /app/entrypoint.sh

COPY --from=backend-build /source/backend/backend-assembly /app/backend
COPY --from=frontend-build /source/frontend/dist /app/frontend
COPY --from=scala-213-bridge-build /source/scala-213-bridge/scala213bridge.jar /app/scala213bridge.jar
COPY --from=scala-212-bridge-build /source/scala-212-bridge/scala212bridge.jar /app/scala212bridge.jar
COPY --from=scala-3-bridge-build /source/scala-3-bridge/scala3bridge.jar /app/scala3bridge.jar

ENV SCALA_213_BRIDGE /app/scala213bridge.jar
ENV SCALA_212_BRIDGE /app/scala212bridge.jar
ENV SCALA_3_BRIDGE /app/scala3bridge.jar

EXPOSE 80

CMD ["/app/entrypoint.sh"]

