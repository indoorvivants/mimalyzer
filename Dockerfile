FROM node:22 as build

WORKDIR /usr/local/bin

RUN wget https://raw.githubusercontent.com/VirtusLab/scala-cli/main/scala-cli.sh && \
    mv scala-cli.sh scala-cli && \
    chmod +x scala-cli && \
    scala-cli config power true && \
    scala-cli version && \
    echo '@main def hello = println(42)' | scala-cli run _ --js -S 3.5.0-RC2

WORKDIR /source
COPY shared shared

WORKDIR /source/frontend
COPY frontend/ .
RUN npm install && npm run build

WORKDIR /source/backend
COPY backend/ .
RUN scala-cli package . --assembly -f -o ./backend-assembly

FROM nginx

RUN apt update && apt install -y gpg wget && \
    wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null && \
    echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list && \
    apt update && apt install -y temurin-22-jdk

COPY ./nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY ./nginx/entrypoint.sh /app/entrypoint.sh
COPY --from=build /source/backend/backend-assembly /app/backend
COPY --from=build /source/frontend/dist /app/frontend

EXPOSE 80

CMD ["/app/entrypoint.sh"]

