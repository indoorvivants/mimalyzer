# Mimalyzer

**https://mimalyzer.fly.dev/**

Quickly verify whether the change you're about to make is binary compatible in Scala and tasty-compatible in Scala 3.

![image](https://github.com/user-attachments/assets/b57c7f7e-515a-4b93-ae68-4373c6311842)

Built from my fullstack template: https://github.com/indoorvivants/scala-cli-smithy4s-fullstack-template.

If you want to have it running locally, just run `make docker` and it will produce a docker container `mimalyzer:latest`.

## Developing

Definitely run `make setup-ide` first! [Metals](https://scalameta.org/metals/) is recommended.

I recommend installing [mprocs](https://github.com/pvolok/mprocs) and then running `make setup-ide && mprocs` at the root of the project – this will setup a fully 
live reloaded environment, with the app available on `http://localhost:5173`.

Otherwise, take a look at mprocs.yaml to see the commands that you need to run. For simplest changes, `make run-backend` and `make run-frontend` should be enough.
