# CookSync — Dockerization Plan

**Status:** Proposal — not yet implemented
**Author:** Yaron Serlin
**Scope:** `cook-sync-server` (Spring Boot API) + its MySQL database

## 1. Why

The server currently only runs from a developer machine: a locally installed MySQL instance, a manually created `.env`, and `./mvnw spring-boot:run`. Every new environment (a teammate's laptop, a CI runner, a future staging server) repeats that manual setup by hand, and "works on my machine" drift is easy (MySQL version, JDK version, missing env var).

Dockerizing the server gives us:
- One command (`docker compose up`) to get the API + database running, identical on every machine.
- A reproducible build (fixed JDK 21, fixed MySQL version) instead of whatever happens to be installed locally.
- A packaged artifact (a Docker image) that can later be deployed to any host or cloud service without re-doing setup.

## 2. Scope

| Component | Dockerize? | Notes |
|---|---|---|
| `cook-sync-server` | Yes | Spring Boot 3.4.2, Java 21, Maven |
| MySQL database | Yes | Runs as a container instead of a local install |
| `cooksync-DTOs` | Built, not shipped | It's a Maven dependency the server needs at *build* time (installed into the local Maven repo). It gets built inside the server's Docker build, not run as its own service. |
| `cook-sync-client` | **No** | It's an Android app that runs on a phone/emulator, not a server process — there's nothing to containerize. Out of scope. |

## 3. What gets added to the repo

```
cook-sync-server/
  Dockerfile          # multi-stage build: compile with Maven, run with a slim JRE
  .dockerignore        # excludes target/, .env, .git, IDE files from the build context
docker-compose.yml      # orchestrates the server + MySQL containers together
.env.example            # documents required variables without real secrets (already close to this via the current .env)
```

No existing source code changes — this is purely additive tooling.

### 3.1 `cook-sync-server/Dockerfile` (multi-stage)

1. **Build stage** (`maven:3.9-eclipse-temurin-21`):
   - Copy `cooksync-DTOs`, run `mvn install` to publish the DTO jar into the build's local Maven repo (mirrors what the README already tells developers to do by hand).
   - Copy `cook-sync-server`, run `mvn package` to produce the Spring Boot fat jar.
2. **Runtime stage** (`eclipse-temurin:21-jre-alpine`, or similar slim base):
   - Copy only the built jar from the build stage.
   - `ENTRYPOINT ["java", "-jar", "app.jar"]`.
   - Result: the final image doesn't carry Maven or build caches, just the JRE + jar.

### 3.2 `docker-compose.yml`

Two services:

- **`mysql`** — official `mysql:8` image, a named volume for data persistence (`mysql_data:/var/lib/mysql`), a healthcheck so the server waits for MySQL to be ready before starting, database name/user/password sourced from `.env`.
- **`server`** — built from `cook-sync-server/Dockerfile`, `depends_on: mysql` (condition: healthy), port `8080` published to the host, environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `MAIL_*`, `CLOUDINARY_*`) passed through from a `.env` file at the repo root via compose's `env_file:`.

Key detail: `DB_URL` inside the container must point at the MySQL **service name** (`jdbc:mysql://mysql:3306/cooksync_db...`), not `localhost` — `localhost` inside a container refers to the container itself, not the host or the other container. This is a one-line env var change, no code change (the URL is already externalized via `${DB_URL:...}` in `application.properties:10`).

Flyway migrations need no changes — they already run automatically on server startup against whatever `DB_URL` points to.

## 4. Secrets handling

- `.env` (with real values) stays **out of git**, same as today — compose just reads it the same way Spring Boot's `spring.config.import=optional:file:.env` does now.
- `.env.example` gets committed with the variable names and safe placeholder/default values (`JWT_SECRET`, `DB_PASSWORD`, `CLOUDINARY_*`, `MAIL_*`) so a new teammate knows what to fill in — no real secrets in the file.
- Nothing secret gets baked into the image itself; everything is injected at container-start time via env vars.

## 5. Developer workflow after this change

```bash
cp .env.example .env      # fill in real values once
docker compose up --build  # builds images, starts MySQL + server
```

That replaces the current 4-step manual README setup (create DB by hand, `mvn install` the DTOs, hand-write `.env`, `./mvnw spring-boot:run`) with two commands. The existing manual/`mvnw` workflow keeps working unchanged for anyone who prefers it — Docker is an additional option, not a replacement requirement.

## 6. Out of scope for this first pass (possible follow-ups)

- **CI/CD**: building/pushing the image in GitHub Actions, image registry choice. Separate proposal once the base setup is approved.
- **Deployment target** (staging/production hosting — a VM, ECS/Cloud Run/Fly.io, Kubernetes, etc.) — needs a decision on where this actually runs before planning that.
- **Hot-reload for local dev** (bind-mounting source + Spring DevTools) — nice-to-have, adds complexity; can be a phase 2 if the team wants to develop primarily inside containers rather than just running the DB in one.
- **Android client** — not applicable, as noted above.

## 7. Open questions for review

1. Is this only meant to standardize **local development**, or is a deployment target (staging/prod) also expected soon? That changes how much to invest in the compose setup vs. a proper CI/CD + registry pipeline.
2. Any preference on base images (Alpine vs. Debian-slim JRE) or MySQL version pinning (`mysql:8` vs. an exact patch version)?
3. Should the optional Mail/Cloudinary integrations stay optional in the container setup too (as they are today — app logs a warning and continues if unset), or should compose enforce they're set?
4. OK to commit a `.env.example` with variable names visible (no secret values), as described in §4?

## 8. Estimated effort

Roughly half a day to a day: write + test the Dockerfile and compose file, verify Flyway/migrations run cleanly against the containerized MySQL, verify JWT/mail/Cloudinary env passthrough, update the README with the new quick-start option.
