# cookSync

A mobile (Android) app for sharing and discovering cooking recipes. Users browse and search recipes, save favorites, write personal notes on preparation steps, follow a guided step-by-step "Cooking Mode" with timers, rate and review recipes, and create/publish their own recipes through a guided 4-step wizard. Admin users get a moderation console for managing users, tags, measurement units, and reported reviews.

This file is self-contained: it covers everything needed to build, run, and understand the project as a whole. Each module below also has its own standalone README with module-specific detail.

## Repository layout

| Module | Description |
|---|---|
| [`cook-sync-client`](cook-sync-client) | The Android app (Java, MVVM) |
| [`cook-sync-server`](cook-sync-server) | The REST backend (Spring Boot) |
| [`cooksync-DTOs`](cooksync-DTOs) | Shared DTO library used by both client and server |

## Main technologies

**Client (Android, Java 17):** Retrofit + OkHttp + Gson, MVVM with `ViewModel`/`LiveData`, Glide / Cloudinary Android SDK / Fresco / PhotoView for images, `androidx.security` for encrypted local session storage.

**Server (Java 21, Spring Boot 3.4.2):** Spring Web, Spring Data JPA + MySQL, Spring Security + JJWT (stateless JWT auth), Flyway for schema migrations, Cloudinary SDK, Gmail API (OAuth2, HTTPS) for transactional email, Lombok.

**Shared:** `cooksync-DTOs` — a small Maven module holding every request/response class, installed locally (`mavenLocal()`) and consumed identically by both sides.

## Running locally — quick start

The project has three modules that must be built/run in order: the shared DTOs library first, then the server, then the client.

```bash
# 1. Create an empty MySQL database
mysql -u root -p -e "CREATE DATABASE cooksync_db;"

# 2. Build the shared DTOs library and install it locally
cd cooksync-DTOs && mvn install && cd ..

# 3. Create a .env file in cook-sync-server (see cook-sync-server/README.md for the full variable table)
#    JWT_SECRET is required; DB_URL/DB_USERNAME/DB_PASSWORD, GOOGLE_OAUTH_*
#    and CLOUDINARY_* can be tuned for your local environment.

# 4. Run the server (Flyway applies the schema migrations automatically)
cd cook-sync-server && ./mvnw spring-boot:run
```

Once the server is running, open `cook-sync-client` in Android Studio, confirm that `BASE_URL` (in `app/build.gradle.kts`) points to the right server address (`http://10.0.2.2:8080/` for the emulator, or your machine's local IP for a physical device), and run the app.

### Running the server with Docker (alternative)

Instead of installing MySQL locally and running Maven by hand, the server and its database can run in containers via Docker Compose:

```bash
cp .env.example .env   # fill in JWT_SECRET and CLOUDINARY_* at minimum
./docker-up.sh
```

This builds `cooksync-DTOs` and `cook-sync-server` inside the image and starts MySQL alongside it (Flyway still applies schema migrations automatically on startup). The API is reachable at `http://localhost:8080` (or `http://10.0.2.2:8080/` from the Android emulator). See `.env.example` for the full list of variables.

Add `--seed` to wipe and repopulate the database with the demo dataset (30 recipes, 15 users) on startup — `./docker-up.sh --seed`. Without it, the server starts normally with whatever data is already in the database.
