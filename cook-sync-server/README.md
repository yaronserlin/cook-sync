# cook-sync-server

The REST backend for CookSync, built with Spring Boot 3.4.2 on Java 21. It exposes the API the [`cook-sync-client`](../cook-sync-client) Android app talks to, backed by MySQL and JWT-based stateless authentication.

For the full technical writeup (architecture, ER diagram, API reference) see [`../COOKSYNC_USER_GUIDE.md`](../COOKSYNC_USER_GUIDE.md) (Hebrew). For the request/response payload shapes, see the shared [`cooksync-DTOs`](../cooksync-DTOs) module.

## Tech stack

- **Spring Web** — REST controllers
- **Spring Data JPA** + **MySQL** (`mysql-connector-j`) — persistence
- **Flyway** — versioned schema migrations (`src/main/resources/db/migration`)
- **Spring Security** + **JJWT** — stateless JWT authentication/authorization
- **Spring Mail** (Gmail SMTP) — OTP/registration/password-reset emails
- **Cloudinary SDK** — signed direct-to-cloud image uploads for recipe/avatar photos
- **Lombok** — boilerplate reduction
- **cooksync-DTOs** — shared request/response classes, installed locally via `mavenLocal()`

## Package layout (`com.cooksync_server`)

| Package | Contents |
|---|---|
| `controllers` | REST endpoints — one controller per resource (see [API overview](#api-overview)) |
| `services` | Business logic, one interface + `*Imp` implementation per service |
| `entities` | JPA entities (`User`, `Recipe`, `Review`, `Tag`, `Unit`, tokens, etc.) |
| `repositories` | Spring Data JPA repositories, plus `RecipeSpecifications` for dynamic search/filter queries |
| `mappers` | Entity ↔ DTO conversion |
| `config` | Security (`SecurityConfig`, `JwtUtil`, JWT filter/entry point), Cloudinary, Flyway, CORS/web config, request logging, and `DataSeeder`/`SkillRecipeDataSeeder` for local dev seed data |
| `exceptions` | `GlobalExceptionHandler` plus domain exceptions (`ResourceNotFoundException`, `auth/*` for credential/OTP failures, etc.) |

## API overview

All routes are prefixed `/api`. Grouped by controller:

| Controller | Base path | Purpose |
|---|---|---|
| `AuthController` | `/api/auth` | Registration + OTP verification, login, token refresh/logout, profile/avatar/email/password updates, privacy settings, account deactivation/deletion, forgot/reset password |
| `RecipeController` | `/api/recipes` | Paged listing, search, browse by tag, "my recipes", create/update/delete, visibility toggle |
| `IngredientController` / `InstructionController` | `/api/recipes/{id}/...` | Add/edit/delete ingredients and instructions on a recipe |
| `ReviewController` | `/api/recipes/{id}/reviews`, `/api/reviews` | List/create/delete reviews, report a review |
| `NoteController` | `/api/notes` | Personal per-user notes on recipe instructions |
| `FavoriteController` | `/api/favorites` | List/add/remove favorite recipes |
| `TagsController` | `/api/tags` | List tags, popular tags, create custom tags |
| `UnitController` | `/api/units` | List/create/delete measurement units |
| `UserController` | `/api/users` | Public user profile, a user's recipes/favorites |
| `CloudinaryController` | `/api/cloudinary` | Signed upload signature + base folder for direct client uploads |
| `AdminController` | `/api/admin` | Moderation console: stats, user list/suspend/enable/delete, reported reviews, duplicate-tag detection/merge |

## Configuration

Environment variables are read via `application.properties` (`src/main/resources/application.properties`), which also loads a local `.env` file if present (`spring.config.import=optional:file:.env[.properties]`).

| Variable | Required | Purpose |
|---|---|---|
| `JWT_SECRET` | Yes | Signing key for access/refresh tokens — no insecure fallback |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | No (default to local MySQL, `root`/`root`) | MySQL connection |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Yes (for image uploads) | Cloudinary account credentials |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | No | Gmail SMTP address + [App Password](https://myaccount.google.com/apppasswords). If unset, emails are logged instead of sent |
| `MAIL_HOST`, `MAIL_PORT` | No | Default to `smtp.gmail.com:587` |
| `CORS_ALLOWED_ORIGINS` | No | Comma-separated allowed origins, defaults to `*` |
| `PORT` | No | Defaults to `8080` |

Schema is managed entirely by Flyway (`db/migration/V1__init_schema.sql`) — `ddl-auto` is set to `update` only as a safety net, not the source of truth.

## Running locally

```bash
# 1. Create an empty MySQL database
mysql -u root -p -e "CREATE DATABASE cooksync_db;"

# 2. Build and install the shared DTOs module (run from the repo root)
cd ../cooksync-DTOs && mvn install && cd ../cook-sync-server

# 3. Create a .env file here with at least JWT_SECRET
#    (see the Configuration table above and the full guide for the rest)

# 4. Run — Flyway applies migrations automatically on startup
./mvnw spring-boot:run
```

## Tests

```bash
./mvnw test
```

Tests live under `src/test/java/com/cooksync_server`, covering services and controllers with `spring-boot-starter-test` + `spring-security-test`.
