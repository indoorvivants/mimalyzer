# Build scala-cli container

FROM node:22 as scala-cli-build

WORKDIR /usr/local/bin
COPY ./frontend/project.scala /tmp/frontend-v.scala
# Extract Scala version from a file that contains a line "//> using scala 3.7.3"
RUN wget https://raw.githubusercontent.com/VirtusLab/scala-cli/main/scala-cli.sh && \
    mv scala-cli.sh scala-cli && \
    chmod +x scala-cli && \
    scala-cli config power true && \
    scala-cli version && \
    echo '@main def hello = println(42)' | scala-cli run _ --js -S 3.7.3

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
RUN scala-cli package . --library -f -o ./scala213bridge.jar

# Build scala212 compiler bridge

FROM scala-cli-build as scala-212-bridge-build

WORKDIR /source/compiler-interface
COPY compiler-interface/ .

WORKDIR /source/scala-212-bridge
COPY scala-212-bridge/ .
RUN scala-cli package . --library -f -o ./scala212bridge.jar


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
    apt update && apt install -y temurin-24-jdk make

RUN curl -fLo coursier https://github.com/coursier/launchers/raw/master/coursier && \
    chmod +x coursier && \
    mv ./coursier /usr/local/bin/cs && \
    cs --help

WORKDIR /app

COPY Makefile .

RUN make prepare-classpaths

ENV SCALA_213_CLASSPATH_FILE /app/.dev/scala213.classpath
ENV SCALA_213_COMPILER_CLASSPATH_FILE /app/.dev/scala213.compiler.classpath

ENV SCALA_212_CLASSPATH_FILE /app/.dev/scala212.classpath
ENV SCALA_212_COMPILER_CLASSPATH_FILE /app/.dev/scala212.compiler.classpath

ENV SCALA_3_CLASSPATH_FILE /app/.dev/scala3.classpath
ENV SCALA_3_COMPILER_CLASSPATH_FILE /app/.dev/scala3.compiler.classpath

ENV SCALA_213_BRIDGE /app/.dev/scala213bridge.jar
ENV SCALA_212_BRIDGE /app/.dev/scala212bridge.jar
ENV SCALA_3_BRIDGE /app/.dev/scala3bridge.jar

COPY ./nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY ./nginx/entrypoint.sh /app/entrypoint.sh

COPY --from=backend-build /source/backend/backend-assembly /app/backend
COPY --from=frontend-build /source/frontend/dist /app/frontend
COPY --from=scala-213-bridge-build /source/scala-213-bridge/scala213bridge.jar /app/.dev/scala213bridge.jar
COPY --from=scala-212-bridge-build /source/scala-212-bridge/scala212bridge.jar /app/.dev/scala212bridge.jar
COPY --from=scala-3-bridge-build /source/scala-3-bridge/scala3bridge.jar /app/.dev/scala3bridge.jar

EXPOSE 80

CMD ["/app/entrypoint.sh"]
