# Mimalyzer

**https://mimalyzer.indoorvivants.com/**

Quickly verify whether the change you're about to make is binary compatible in Scala and tasty-compatible in Scala 3.

<img width="2926" height="1984" alt="image" src="https://github.com/user-attachments/assets/bc377be7-5034-40f3-96b8-f388f6ff357b" />

## Developing

This is a full stack Scala project, using sbt build tool.

1. JDK, sbt, Node.js, and npm must be installed and available
2. Run `npm install` in [./frontend](./frontend)
3. Have Postgres running on `localhost:5432` with user `postgres` having empty password, and `mimalyzer` database pre-created. Alternatively use `PG_*` variables (in [PgCredentials.scala](backend/PgCredentials.scala) to control connection to postgres.
 
The frontend development server and backend are intended to be started separately:

1. For frontend development, run `frontend/reStart` sbt command to start Vite development server, then run `~frontend/fastLinkJS` in sbt shell to continuously re-link the frontend code.
2. For backend development, run `~backend/reStart` to continuously restart the backend API server (by default it binds to port 9977, which is what Vite frontend expects)
3. For API/protocol development, modify `*.smithy` files – when backend restarts the protocol code gets regenerated.

### Docker

Mimalyzer is deployed using a docker container which builds it from scratch, so `docker build . -t mimalyzer` will give you a working mimalyzer instance (assuming connection to postgres is available).

To quickly run locally, run `docker compose up`.

