# Submito

Backend service built with Kotlin and Spring Boot (JPA, Spring Security, JWT). PostgreSQL is the primary data store and runs locally via Docker Compose.

## Tech stack

- Kotlin 2.2 / JDK 24
- Spring Boot 4 (web-mvc, security, data-jpa, validation)
- PostgreSQL 16 (Docker)
- Redis 7 (Docker; dependency present, rate limiting is in-memory for now)
- Gradle (wrapper included in the repo)
- `kotlin("plugin.jpa")` for JPA entity support

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

   Change `JWT_SECRET` to a long random string before running locally.

3. Start Postgres and Redis:

   ```bash
   docker compose up -d
   ```

4. Run the application:

   ```bash
   ./gradlew bootRun
   ```

5. Check health:

   ```bash
   curl http://localhost:8080/health
   ```

   Expected: `Everything is OK`

## Useful commands

| Action                   | Command                                                                                                 |
| ------------------------ | ------------------------------------------------------------------------------------------------------- |
| Stop containers          | `docker compose down`                                                                                   |
| Reset DB (delete volume) | `docker compose down -v`                                                                                |
| Postgres logs            | `docker compose logs -f postgres`                                                                       |
| psql in container        | `docker compose exec postgres psql -U submito -d submito`                                               |
| List users in DB         | `docker compose exec postgres psql -U submito -d submito -c "SELECT id, name, email, role FROM users;"` |

## Environment variables

Settings live in `.env` (gitignored). Template: `.env.example`.

| Variable                                                 | Purpose                                 |
| -------------------------------------------------------- | --------------------------------------- |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | Spring → Postgres                       |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`      | Postgres container init                 |
| `JWT_SECRET`, `JWT_EXPIRATION`                           | JWT signing and lifetime                |
| `SECURITY_USER_NAME`, `SECURITY_USER_PASSWORD`           | Default Spring Security user (dev only) |

Local Postgres uses host port **5433**.

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
# Register
curl -i -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Viktoriia","email":"viktoriia@mail.com","password":"12345abAA!","role":"STUDENT"}'

# Login
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

## Rate limiting

`AuthRateLimitFilter` limits requests per client IP (in-memory):

| Endpoint              | Limit                      |
| --------------------- | -------------------------- |
| `POST /auth/register` | 5 requests per 15 minutes  |
| `POST /auth/login`    | 10 requests per 15 minutes |

Counters reset on app restart. For production with multiple instances, use Redis-backed rate limiting.

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

## Security notes

- Passwords are stored as **BCrypt** hashes, never plain text.
- Registration accepts only `RegisterRole` (`STUDENT`, `TEACHER`); `Role.ADMIN` exists in the DB but cannot be chosen via public register.
- Email is normalized (`lowercase().trim()`) before save and lookup.

## Project structure (main packages)

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
