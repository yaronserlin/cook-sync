# cook-sync-server

The REST backend for CookSync — a recipe-sharing and discovery platform. Built with **Spring Boot 3.4.2** on **Java 21**, it exposes the API the [`cook-sync-client`](../cook-sync-client) Android app talks to, backed by **MySQL** and stateless **JWT** authentication.

This file is self-contained: it covers everything needed to configure, run, and understand this module on its own. For the request/response payload shapes shared with the client, see [`../cooksync-DTOs`](../cooksync-DTOs).

## Tech stack

- **Spring Web** — REST controllers
- **Spring Data JPA** + **MySQL** (`mysql-connector-j`) — persistence
- **Flyway** — versioned schema migrations (`src/main/resources/db/migration/V1__init_schema.sql`)
- **Spring Security** + **JJWT** — stateless JWT authentication/authorization (no server-side session state)
- **Gmail API** (OAuth2, HTTPS) — OTP, registration, and password-reset emails, sent as CookSync's Gmail account (`cooksyncapplication@gmail.com`) (Gmail SMTP is unreachable from Render's network, so mail is sent via a plain HTTPS call instead of `spring-boot-starter-mail`/SMTP)
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
| `config` | `SecurityConfig` (stateless, no CSRF, public-path allowlist, `@EnableMethodSecurity`), `JwtUtil`/`JwtAuthenticationFilter`/`JwtAuthenticationEntryPoint`, `WebConfig` (CORS), `FlywayConfig` (auto-repairs a failed migration before retrying), `CloudinaryConfig`, `RequestAndResponseLoggingFilter`, `RateLimitFilter` (per-IP, per-endpoint fixed-window throttle on the public auth endpoints — login/registration brute-forcing and forgot-password/resend-OTP email-bombing), `FaintDebugMessageConverter` (dims DEBUG/TRACE console output), the `@Profile("seed")`-gated dev-only seeder `DataSeeder`, and `ProductionSeeder` (idempotent, non-destructive; runs whenever either the `--prodSeeder` CLI flag or the `prodSeeder` Spring profile is present) |
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
| `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`, `GOOGLE_OAUTH_REFRESH_TOKEN` | No (all three required together) | Gmail API OAuth2 credentials for sending as CookSync's Gmail account (`cooksyncapplication@gmail.com`). If unset, OTP/reset codes are logged instead of emailed. Obtain by: creating a Google Cloud project, enabling the Gmail API, configuring an OAuth consent screen (External, scope `gmail.send` only, with `cooksyncapplication@gmail.com` added as a test user), creating a "Desktop app" OAuth client, then running the one-time authorization flow (`https://accounts.google.com/o/oauth2/v2/auth?...&access_type=offline&prompt=consent`, completed while signed in as `cooksyncapplication@gmail.com`) and exchanging the resulting code at `https://oauth2.googleapis.com/token` for a refresh token. **Caveat:** a consent screen left in "Testing" status (the default, zero-review-required option) issues refresh tokens that expire after about a week — fine for local dev, but an unattended production deployment needs the consent screen moved to "Production" via Google's app-verification review (requires a public privacy-policy URL) to avoid the token expiring on its own |
| `CORS_ALLOWED_ORIGINS` | No (defaults to `*` in `dev`, closed in `prod`) | Comma-separated allowed origins. Irrelevant to the Android client — CORS is a browser-only mechanism, not enforced by Retrofit/OkHttp — so this only matters if a browser-based client is ever added |
| `PORT` | No (defaults to `8080`) | Port the server listens on |
| `SPRING_PROFILES_ACTIVE` | No (no default — see below) | `dev` or `prod` (see below); combine with `prodSeeder` (e.g. `prod,prodSeeder`) to also run `ProductionSeeder` on that boot |

Schema is managed entirely by Flyway (`src/main/resources/db/migration/V1__init_schema.sql`).

### `dev` / `prod` profiles

`application.properties` sets no default active profile, and its own settings are already the safe, production-appropriate values — so an environment that never sets `SPRING_PROFILES_ACTIVE` (a misconfigured deploy, a future deploy target) fails safe instead of silently getting noisier/more permissive behavior. `application-dev.properties` (loaded only when `dev` is explicitly active) relaxes these for local convenience; `application-prod.properties` re-states the same values as explicit documentation, plus one prod-only setting. Activate `dev` locally with `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`, or a real `SPRING_PROFILES_ACTIVE=dev` environment variable (shell profile or IDE run config) — **not** by adding `spring.profiles.active=dev` to your local `.env`, which does not work (properties from a `spring.config.import`-ed file aren't honored for profile activation, only for later placeholder resolution — verified empirically, it silently falls back to Spring's own "default" profile instead).

| Setting | base (no profile / any unlisted profile) | `dev` | `prod` (redundant with base — see above) |
|---|---|---|---|
| `logging.level.com.cooksync_server` | `INFO` | `DEBUG` (incl. redacted request/response bodies) | `INFO` |
| `logging.level.org.springframework` | `INFO` | `INFO` | `WARN` |
| `spring.jpa.hibernate.ddl-auto` | `validate` (schema changes only ever happen through reviewed Flyway migrations) | `update` (Hibernate may auto-adjust the schema while iterating locally) | `validate` |
| `cors.allowed-origins` | empty (closed — there's no browser client; set `CORS_ALLOWED_ORIGINS` if one is ever added) | `*` (open, for local browser-based tooling) | empty |
| `cloudinary.upload.base-folder` | `cooksync-prod` | `cooksync-dev` | `cooksync-prod` |

Deploying to Render: set `SPRING_PROFILES_ACTIVE=prod` as an environment variable on the service — this isn't strictly required for safety anymore (the base defaults already are prod's values), but it's still what enables the `org.springframework=WARN` logging and `server.forward-headers-strategy=native` (needed so `RateLimitFilter`/request-IP-logging see the real client IP instead of Render's edge proxy). Set it alongside `JWT_SECRET`, `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`, and `GOOGLE_OAUTH_*`/`CLOUDINARY_*` if those features are needed in production. Also set Render's own **Health Check Path** dashboard setting to `/actuator/health` — separate from anything in this repo, and needed so Render can tell a bad deploy apart from a good one before routing traffic to it.

### Health check

Spring Boot Actuator exposes `GET /actuator/health` (only that one endpoint — `management.endpoints.web.exposure.include=health` — everything else Actuator can expose, e.g. `/env`, `/beans`, stays off since it'd leak internal config), permitted unauthenticated in `SecurityConfig` since neither Docker's nor Render's prober sends a JWT. `management.endpoint.health.show-details=never` keeps the response to a bare `{"status": "UP"}`/`{"status": "DOWN"}` — the DB-connectivity check backing it still runs and still flips the status, just without exposing *why* to an anonymous caller. The Dockerfile's own `HEALTHCHECK` instruction polls it every 30s (`docker compose ps` / `docker inspect` show the result); `docker-compose.yml` doesn't redeclare one since Compose picks up an image's built-in `HEALTHCHECK` automatically when a service doesn't specify its own.

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

There's also `ProductionSeeder`, a small, hand-picked recipe set with real pre-existing Cloudinary image URLs, meant for production use rather than local dev: unlike `DataSeeder`, it never truncates the database and is idempotent (units, tags, the creator account, and each recipe are only inserted if not already present, so running it repeatedly is safe). It's always registered as a bean, but does nothing unless *either* the literal `--prodSeeder` CLI argument is passed (e.g. `./mvnw spring-boot:run -Dspring-boot.run.arguments=--prodSeeder`) *or* the `prodSeeder` Spring profile is active (e.g. `SPRING_PROFILES_ACTIVE=prod,prodSeeder` — the practical option on Render, where toggling an environment variable is easier than editing the container's start command). Not wired into `docker-up.sh`.

Verified working end-to-end: `docker compose up --build` brings up a healthy MySQL 8.4 container and a server container that applies all Flyway migrations and starts Tomcat on port 8080, correctly enforcing JWT authentication on protected endpoints.

## Tests

```bash
./mvnw test
```

27 test classes under `src/test/java/com/cooksync_server`, covering services and controllers with `spring-boot-starter-test` + `spring-security-test`.
