# cook-sync-server

The REST backend for CookSync — a recipe-sharing and discovery platform. Built with **Spring Boot 3.4.2** on **Java 21**, it exposes the API the [`cook-sync-client`](../cook-sync-client) Android app talks to, backed by **MySQL** and stateless **JWT** authentication.

This file is self-contained: it covers everything needed to configure, run, and understand this module on its own. For the full user-facing feature walkthrough and a deeper architecture/database write-up (with diagrams), see [`../doc/להגשה/מסמך תיאור פונקציונלי.docx`](../doc); for the request/response payload shapes shared with the client, see [`../cooksync-DTOs`](../cooksync-DTOs).

## Tech stack

- **Spring Web** — REST controllers
- **Spring Data JPA** + **MySQL** (`mysql-connector-j`) — persistence
- **Flyway** — versioned schema migrations (`src/main/resources/db/migration/V1__init_schema.sql`)
- **Spring Security** + **JJWT** — stateless JWT authentication/authorization (no server-side session state)
- **Spring Mail** (Gmail SMTP) — OTP, registration, and password-reset emails
- **Cloudinary SDK** — signed, direct-to-cloud image uploads for recipe/avatar photos (the server never touches the image bytes)
- **Lombok** — boilerplate reduction
- **cooksync-DTOs** — shared request/response classes, resolved from the local Maven repository (`mavenLocal()`/`~/.m2`)

## Package layout (`com.cooksync_server`)

| Package | Contents |
|---|---|
| `controllers` | REST endpoints — one controller per resource (see [API overview](#api-overview)) |
| `services` | Business logic: one interface + `*Imp` implementation per domain service, plus small shared helpers (`SessionIssuer` issues every access+refresh token pair; `CredentialVerifier` re-checks the caller's current password before a password/email change or account deletion; `OwnershipValidator` centralizes "does this user own this resource, or are they admin" checks; `OtpCodeGenerator`, `PagedResponseMapper`, `RecipeImageUtils`) |
| `entities` | JPA entities — `User`, `Recipe`, `Ingredient`, `Instruction`, `DescriptionBlock`, `RecipeImage`, `Tag`, `Review`, `ReviewReport`, `FavoriteRecipe`, `PersonalInstructionNote`, `Unit`, and the token/pending-registration entities (`RefreshToken`, `PasswordResetToken`, `EmailChangeToken`, `PendingRegistration`) |
| `repositories` | Spring Data JPA repositories, plus `RecipeSpecifications` for dynamic search/filter queries |
| `mappers` | Entity ↔ DTO conversion — `RecipeMapper`, `UserMapper`, `AdminMapper`, `IngredientMapper`, `InstructionMapper`, `ReviewMapper`, `TagMapper`, `UnitMapper` |
| `config` | `SecurityConfig` (stateless, no CSRF, public-path allowlist, `@EnableMethodSecurity`), `JwtUtil`/`JwtAuthenticationFilter`/`JwtAuthenticationEntryPoint`, `WebConfig` (CORS), `FlywayConfig` (auto-repairs a failed migration before retrying), `CloudinaryConfig`, `RequestAndResponseLoggingFilter`, `FaintDebugMessageConverter` (dims DEBUG/TRACE console output), and the `@Profile`-gated dev-only seeders `DataSeeder` / `SkillRecipeDataSeeder` |
| `exceptions` | `GlobalExceptionHandler` (single `@RestControllerAdvice` turning every exception — custom or framework — into one uniform JSON error shape) plus the domain exceptions it handles: `ResourceNotFoundException`, `ResourceAlreadyExistsException`, `ResourceInUseException`, `UnauthorizedActionException`, `InvalidCredentialsException`, `UserAlreadyExistsException`, `InvalidOtpException`, `OtpExpiredException`, `TooManyOtpAttemptsException` |
| `constants` | Shared literals — `ApiRoutes` (path constants used by both controllers and `SecurityConfig`'s public-path allowlist) |

Two `@Scheduled` jobs run daily: `AccountPurgeScheduler` (03:00) permanently deletes accounts past their 30-day self-deletion grace period, and `PendingRegistrationCleanupScheduler` (03:30) deletes registrations that were never OTP-verified and have expired.

## API overview

All routes are prefixed `/api`. Grouped by controller:

| Controller | Base path | Purpose | Auth |
|---|---|---|---|
| `AuthController` | `/api/auth` | Register + verify/resend OTP, login, refresh/logout, forgot/reset password, profile/avatar/email/password updates, privacy settings, deactivation/deletion | Public for register/login/refresh/forgot/reset/OTP; the rest requires a valid access token |
| `RecipeController` | `/api/recipes` | Paged listing, search, browse by tag, "my recipes", create/update/delete, visibility toggle | Authenticated |
| `IngredientController` / `InstructionController` | `/api` | Add/update/delete a recipe's ingredients or instructions | Authenticated + must own the parent recipe (or be admin) |
| `ReviewController` | `/api` | List/create/delete reviews on a recipe, report a review | Authenticated |
| `NoteController` | `/api/notes` | Personal, per-user notes on a recipe or a specific instruction step | Authenticated |
| `FavoriteController` | `/api/favorites` | List/add/remove favorite recipes | Authenticated |
| `TagsController` | `/api/tags` | List tags, popular tags, create a custom tag | Authenticated |
| `UnitController` | `/api/units` | List measurement units (any authenticated user); create/delete (admin only) | Authenticated (mutations: admin) |
| `UserController` | `/api/users` | A user's public profile, recipes, and favorites — gated by that user's own privacy settings | Authenticated |
| `CloudinaryController` | `/api/cloudinary` | Signed upload signature + base folder, for direct client → Cloudinary uploads | Authenticated |
| `AdminController` | `/api/admin` | Moderation console: stats, user list/suspend/enable/delete, reported reviews, duplicate-tag detection/merge | Admin only (`@PreAuthorize`) |

Errors are always a uniform JSON shape: `{"success": false, "data": null, "error": {"status": ..., "errorCode": "...", "message": "..."}, "message": null}`.

## Configuration

Environment variables are read via `application.properties`, which also auto-loads a local `.env` file if present in this directory (`spring.config.import=optional:file:.env[.properties]`).

| Variable | Required? | Purpose |
|---|---|---|
| `JWT_SECRET` | **Yes** — no fallback | Base64 HMAC-SHA256 signing key (≥ 256 bits) for access/refresh tokens. Generate one with `openssl rand -base64 32` |
| `JWT_REFRESH_EXPIRATION_MS` | No (defaults to `604800000`, i.e. 7 days) | How long a refresh token stays valid; each successful refresh rotates it, so this is the max time a device can stay logged out before needing to sign in again |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | No (default to `jdbc:mysql://localhost:3306/cooksync_db`, `root`/`root`) | MySQL connection |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | No at startup (all default to blank) — needed in practice for image uploads to work | Cloudinary account credentials |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | No | Gmail SMTP address + [App Password](https://myaccount.google.com/apppasswords) (not the regular account password). If unset, OTP/reset codes are logged instead of emailed |
| `MAIL_HOST`, `MAIL_PORT` | No | Default to `smtp.gmail.com:587` |
| `CORS_ALLOWED_ORIGINS` | No (defaults to `*`) | Comma-separated allowed origins |
| `PORT` | No (defaults to `8080`) | Port the server listens on |

Schema is managed entirely by Flyway (`src/main/resources/db/migration/V1__init_schema.sql`) — `spring.jpa.hibernate.ddl-auto=update` is set only as a safety net alongside it, not as the source of truth.

## Running locally

```bash
# 1. Create an empty MySQL database
mysql -u root -p -e "CREATE DATABASE cooksync_db;"

# 2. Build and install the shared DTOs module (only needed once, or after a DTO change)
cd ../cooksync-DTOs && mvn install && cd ../cook-sync-server

# 3. Create a .env file here with at least JWT_SECRET
#    (see the Configuration table above for the rest)

# 4. Run — Flyway applies migrations automatically on startup
./mvnw spring-boot:run
```

The server listens on `0.0.0.0:8080` by default. Verified working: `mvn clean package -DskipTests` builds cleanly, and Flyway applies the full schema to a fresh database without errors.

### Running with Docker instead

From the repo root (not this directory) — brings up MySQL and this server together, no local JDK/Maven/MySQL required:

```bash
cp .env.example .env   # fill in JWT_SECRET at minimum; see the file's own comments
./docker-up.sh
```

This still requires `mvn install` to have been run once in `cooksync-DTOs` on the host machine first — Gradle (for the client) resolves that dependency via `mavenLocal()` on the host, not inside the container, but the server container's own image build handles its own `mvn install`/`mvn package` internally. Add `--seed` to wipe and repopulate the database with the demo dataset (`DataSeeder`: units, tags, ~15 users, ~30 recipes) on startup; without it the server starts with whatever is already in the database. `docker compose down -v` tears down the containers and deletes the `mysql_data` volume (full, irreversible reset).

There's also a second, lighter dev-only seeder, `SkillRecipeDataSeeder`, behind the `seed-skill` profile (not wired into `docker-up.sh` — run it manually with `./mvnw spring-boot:run -Dspring-boot.run.profiles=seed-skill` against the manual setup). It seeds a small, hand-picked recipe set with real pre-existing Cloudinary image URLs instead of the full demo dataset. The two seed profiles are mutually exclusive.

Verified working end-to-end: `docker compose up --build` brings up a healthy MySQL 8.4 container and a server container that applies all Flyway migrations and starts Tomcat on port 8080, correctly enforcing JWT authentication on protected endpoints.

## Tests

```bash
./mvnw test
```

27 test classes under `src/test/java/com/cooksync_server`, covering services and controllers with `spring-boot-starter-test` + `spring-security-test`.
