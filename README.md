# Submito

Backend service built with Kotlin and Spring Boot (JPA, Spring Security, JWT). PostgreSQL is the primary data store. Infrastructure runs via Docker Compose; the API can run on the host (development) or inside Docker (full stack).

## Tech stack

- Kotlin 2.2 / JDK 24
- Spring Boot 4 (web-mvc, security, data-jpa, validation)
- PostgreSQL 16 (Docker)
- Redis 7 (Docker; dependency present, rate limiting is in-memory for now)
- Gradle (wrapper included)
- Docker + Docker Compose (`Dockerfile` multi-stage build for the app)

## Requirements

- [Docker](https://www.docker.com/) + Docker Compose
- JDK 24 (only for local `./gradlew bootRun`)
- Free ports: **5433** (Postgres on host), **6379** (Redis), **8080** (API)

## One-time setup

1. Clone and enter the project:

   ```bash
   git clone <repo-url>
   cd Submito
   ```

2. Create `.env` from the template:

   ```bash
   cp .env.example .env
   ```

3. Edit `.env`: set `JWT_SECRET` to a long random string (not the placeholder).

Configuration lives in `src/main/resources/application.yml` (used by both `bootRun` and the Docker image).

---

## How to run

| Mode              | App                        | Postgres / Redis     | Best for                        |
| ----------------- | -------------------------- | -------------------- | ------------------------------- |
| **Full stack**    | Docker (`app`)             | Docker               | One command, CI-like check      |
| **Development**   | Host (`./gradlew bootRun`) | Docker only          | Daily coding (faster iteration) |
| **Services only** | Not started                | Docker (one or both) | DB tools or custom app start    |

**Database URL depends on where the app runs:**

| App runs on            | `DATABASE_URL`                                                          |
| ---------------------- | ----------------------------------------------------------------------- |
| Host (`bootRun`)       | `jdbc:postgresql://localhost:5433/submito` (from `.env`)                |
| Docker (`app` service) | `jdbc:postgresql://postgres:5432/submito` (set in `docker-compose.yml`) |

Postgres data is stored in the Docker volume `submito_pgdata`. `docker compose down` keeps data; `docker compose down -v` wipes it.

---

### Full stack (app + Postgres + Redis)

Builds the app image and starts all services. Postgres must pass its health check before `app` starts.

```bash
docker compose up --build
```

Background:

```bash
docker compose up --build -d
```

Check:

```bash
curl http://localhost:8080/health
```

Expected: `Everything is OK`

Stop (containers removed, volumes kept):

```bash
docker compose down
```

**After backend code changes**, rebuild and restart only the app:

```bash
docker compose up --build app
```

Postgres and Redis do not need a rebuild when only Kotlin sources change.

---

### Development mode (recommended while coding)

Run infrastructure in Docker; run Spring Boot on your machine for faster feedback.

**Terminal 1 — Postgres + Redis:**

```bash
docker compose up -d postgres redis
```

**Terminal 2 — application:**

```bash
./gradlew bootRun
```

Ensure `.env` contains:

```env
DATABASE_URL=jdbc:postgresql://localhost:5433/submito
```

Check:

```bash
curl http://localhost:8080/health
```

Stop infrastructure:

```bash
docker compose down
```

---

### Run services separately

**Postgres only:**

```bash
docker compose up -d postgres
```

**Redis only:**

```bash
docker compose up -d redis
```

**App only** (starts Postgres via `depends_on`; build required first):

```bash
docker compose up --build app
```

**App on host** (with Postgres already up):

```bash
./gradlew bootRun
```

---

## Docker reference

| Action                        | Command                                                                                                 |
| ----------------------------- | ------------------------------------------------------------------------------------------------------- |
| Full stack (foreground)       | `docker compose up --build`                                                                             |
| Full stack (background)       | `docker compose up --build -d`                                                                          |
| Dev: DB + Redis only          | `docker compose up -d postgres redis`                                                                   |
| Rebuild app after code change | `docker compose up --build app`                                                                         |
| Stop containers               | `docker compose down`                                                                                   |
| Stop and delete DB/Redis data | `docker compose down -v`                                                                                |
| App logs                      | `docker compose logs -f app`                                                                            |
| Postgres logs                 | `docker compose logs -f postgres`                                                                       |
| psql in Postgres container    | `docker compose exec postgres psql -U submito -d submito`                                               |
| List users                    | `docker compose exec postgres psql -U submito -d submito -c "SELECT id, name, email, role FROM users;"` |

**Compose services:**

| Service    | Image / build              | Host port       | Notes                                |
| ---------- | -------------------------- | --------------- | ------------------------------------ |
| `postgres` | `postgres:16`              | `5433` → `5432` | Volume `submito_pgdata`, healthcheck |
| `redis`    | `redis:7-alpine`           | `6379`          | Volume `submito_redisdata`           |
| `app`      | `Dockerfile` (multi-stage) | `8080`          | Waits for healthy Postgres           |

The `Dockerfile` builds a fat JAR with Gradle inside an `eclipse-temurin:24-jdk` stage and runs it on `eclipse-temurin:24-jre`.

---

## Environment variables

Settings live in `.env` (gitignored). Template: `.env.example`.

| Variable                                                 | Purpose                                            |
| -------------------------------------------------------- | -------------------------------------------------- |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | Spring → Postgres (`localhost:5433` for `bootRun`) |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`      | Postgres container init                            |
| `JWT_SECRET`, `JWT_EXPIRATION`                           | JWT signing and lifetime                           |
| `SECURITY_USER_NAME`, `SECURITY_USER_PASSWORD`           | Default Spring Security user (dev only)            |

For the `app` service, `docker-compose.yml` overrides `DATABASE_URL` to use the hostname `postgres` on the Docker network.

---

## API endpoints

| Method | Path             | Description                | Auth                 |
| ------ | ---------------- | -------------------------- | -------------------- |
| `GET`  | `/health`        | Health check               | —                    |
| `POST` | `/auth/register` | Register user, returns JWT | Public               |
| `POST` | `/auth/login`    | Login, returns JWT         | Public               |
| `GET`  | `/users`         | List users (no passwords)  | Public _(temporary)_ |

### Register (`POST /auth/register`)

Request body:

```json
{
  "name": "Viktoriia",
  "email": "viktoriia@mail.com",
  "password": "12345abAA!",
  "role": "STUDENT"
}
```

`role` must be `STUDENT` or `TEACHER` (`ADMIN` is not allowed at registration).

Success: **201 Created** with `accessToken`, `tokenType`, and `user` object.

### Login (`POST /auth/login`)

Request body:

```json
{
  "email": "viktoriia@mail.com",
  "password": "12345abAA!"
}
```

Success: **200 OK** with the same `AuthResponse` shape as register.

Wrong email or password: **401 Unauthorized** with a generic message (does not reveal whether the email exists).

Duplicate email: **409 Conflict**.

Validation errors: **400 Bad Request**.

Rate limit exceeded: **429 Too Many Requests** (see below).

### Example: register then login

```bash
curl -i -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Viktoriia","email":"viktoriia@mail.com","password":"12345abAA!","role":"STUDENT"}'

curl -i -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"viktoriia@mail.com","password":"12345abAA!"}'
```

### Using JWT on protected routes

When endpoints require authentication, send the token from register/login:

```bash
curl http://localhost:8080/users \
  -H "Authorization: Bearer <accessToken>"
```

---

## Rate limiting

`AuthRateLimitFilter` limits requests per client IP (in-memory):

| Endpoint              | Limit                    |
| --------------------- | ------------------------ |
| `POST /auth/register` | 5 requests / 15 minutes  |
| `POST /auth/login`    | 10 requests / 15 minutes |

Counters reset on app restart. For multiple instances in production, use Redis-backed rate limiting.

---

## Error handling

Errors are returned as JSON (`ErrorResponse`: `status`, `error`, `message`, `path`, `timestamp`).

| HTTP status | When                                                        |
| ----------- | ----------------------------------------------------------- |
| `400`       | Validation failed, invalid JSON, invalid `role` in register |
| `401`       | Invalid login credentials                                   |
| `409`       | Email already exists (register or DB unique constraint)     |
| `429`       | Too many register/login attempts from one IP                |
| `500`       | Unhandled server error (default Spring handling)            |

Custom exceptions: `EmailAlreadyExistsException`, `InvalidCredentialsException`, plus handlers for validation and `DataIntegrityViolationException` (race on duplicate email).

---

## Security notes

- Passwords are stored as **BCrypt** hashes, never plain text.
- Registration accepts only `RegisterRole` (`STUDENT`, `TEACHER`); `Role.ADMIN` exists in the DB but cannot be chosen via public register.
- Email is normalized (`lowercase().trim()`) before save and lookup.

---

## Project structure

```
com.example.submito
├── controller/     # REST endpoints
├── service/        # Business logic (register, login)
├── repository/     # JPA repositories
├── entity/         # User, Role, RegisterRole
├── dto/            # Request/response DTOs
├── exception/      # Custom exceptions + GlobalExceptionHandler
└── security/       # SecurityConfig, JwtAuthFilter, AuthRateLimitFilter
```
