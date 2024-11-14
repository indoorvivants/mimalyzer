# Full stack Scala 3

<!--toc:start-->
- [Full stack Scala 3](#full-stack-scala-3)
  - [Pre-requisites](#pre-requisites)
  - [Development workflow](#development-workflow)
    - [Setting up build definitions for Metals](#setting-up-build-definitions-for-metals)
    - [Updating protocol definitions](#updating-protocol-definitions)
    - [Frontend live server](#frontend-live-server)
    - [Backend live server](#backend-live-server)
    - [Packaging as docker container](#packaging-as-docker-container)
    <!--toc:end-->

**Frontend**: [Scala.js](https://www.scala-js.org/) and [Laminar](https://laminar.dev) | **Backend**: [Scala JVM](https://www.scala-lang.org/) and [Http4s](https://http4s.org/) | **Protocol**: [Smithy4s](https://disneystreaming.github.io/smithy4s/) | **Build system**: [Scala CLI](https://scala-cli.virtuslab.org/) and [Vite](https://vitejs.dev/) | **Packaging**: [Docker](https://hub.docker.com/) | **Web server**: [NGINX](https://nginx.org/)

This is an opinionated fullstack scala template:

- The frontend-backend interaction layer is handled through **Smithy4s** protocol definition – this ensures shared modelling and keeps all definitions in sync, additionally allowing for code sharing between frontend and backend
- **Scala CLI** is used everywhere. While it doesn't have multi-module support 
- **NGINX** is used as a web server. Using a very configurable and battle-tested server from the very beginning can help implementing more complicated features later as the app grows. We also use it to serve static assets separately from the JVM backend.
- **Vite and TailwindCSS** are used on the frontend. Bundling a CSS framework can be helpful to get the styling off the ground for people not usually involved in frontend design work.

## Pre-requisites

1. **Frontend**: Node.js and NPM, Scala CLI
2. **Backend**: Scala CLI
3. **Protocol code**: [Coursier](https://get-coursier.io/docs/overview), Scala CLI

## Development workflow

### Setting up build definitions for Metals

Scala CLI doesn't have any support for multi-module projects, so to make sure we can work with this codebase in Metals we need to generate BSP definitions manually.

1. Run `make setup-ide` and then cross your fingers that Metals will pick everything up correctly

### Updating protocol definitions

1. Make changes to `shared/protocol.smithy`
2. Run `make smithy4s`
3. This will regenerate all the code that can be used by both backend and frontend

### Frontend live server

1. Run `make run-frontend`
2. Note that to perform any actions that execute API calls, you need to have backend running as well 

### Backend live server

1. Run `make run-backend`
2. This will run the backend server on port 9999 – which is where Vite's frontend tooling expects it to be

### Packaging as docker container

1. Run `make docker`
2. Note that this project is organised as a self-contained docker image - just running `docker build .` will produce a working docker image. This is particularly useful for services like [Fly.io](https://fly.io/) which detect a Dockerfile and can build and deploy app directly from it
