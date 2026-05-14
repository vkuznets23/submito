# Submito

Backend service built with Kotlin + Spring Boot (JPA, Spring Security, JWT). PostgreSQL is the primary data store and runs locally via Docker Compose.

## Tech stack

- Kotlin 2.2 / JDK 24
- Spring Boot 4 (web-mvc, security, data-jpa, data-redis, validation)
- PostgreSQL 16, Redis 7 (in Docker)
- Gradle (wrapper is included in the repo)

## Requirements

- [Docker](https://www.docker.com/) + Docker Compose
- JDK 24
- Free ports: `5433` (Postgres), `6379` (Redis), `8080` (app)

## Quick start

1. Clone the repository:

   ```bash
   git clone <repo-url>
   cd Submito
   ```

2. Copy the environment template:

   ```bash
   cp .env.example .env
   ```

   Adjust values inside `.env` if needed (e.g. `JWT_SECRET`).

3. Start the database and Redis in the background:

   ```bash
   docker compose up -d
   ```

4. Run the application:

   ```bash
   ./gradlew bootRun
   ```

5. Verify everything is up:

   ```bash
   curl http://localhost:8080/health
   ```

   Expected response: `Everything is OK`.

## Useful commands

| Action                         | Command                                                   |
| ------------------------------ | --------------------------------------------------------- |
| Stop containers                | `docker compose down`                                     |
| Reset DB data (remove volume)  | `docker compose down -v`                                  |
| Tail Postgres logs             | `docker compose logs -f postgres`                         |
| Open psql inside the container | `docker compose exec postgres psql -U submito -d submito` |

## Environment variables

All settings live in `.env` (which is gitignored). The committed template is `.env.example`.

| Variable                                                 | Purpose                            |
| -------------------------------------------------------- | ---------------------------------- |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | Spring connection to Postgres      |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`      | Postgres container initialization  |
| `SECURITY_USER_NAME`, `SECURITY_USER_PASSWORD`           | Default Spring Security user       |
| `JWT_SECRET`, `JWT_EXPIRATION`                           | JWT signing key and token lifetime |

## Endpoints (current)

| Method | Path             | Description                    | Auth                     |
| ------ | ---------------- | ------------------------------ | ------------------------ |
| `GET`  | `/health`        | health check                   | —                        |
| `POST` | `/auth/register` | register a user, returns a JWT | —                        |
| `GET`  | `/users`         | list all users                 | — _(temporarily public)_ |
