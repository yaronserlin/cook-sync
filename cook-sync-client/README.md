# cook-sync-client

The CookSync Android app (Java 17, MVVM). It talks to [`cook-sync-server`](../cook-sync-server) over REST and shares its request/response payload shapes with it via the [`cooksync-DTOs`](../cooksync-DTOs) module.

For the full user-facing feature walkthrough and screenshots, see [`../COOKSYNC_USER_GUIDE.md`](../COOKSYNC_USER_GUIDE.md) (Hebrew).

## Tech stack

- **MVVM** — `ViewModel` + `LiveData`, one package per feature under `ui/`
- **Retrofit** + **OkHttp** + **Gson** — networking, with an `AuthInterceptor`/`TokenAuthenticator` pair handling JWT attachment and silent access-token refresh
- **androidx.security (`security-crypto`)** — encrypted local storage for session tokens
- **Glide**, **Cloudinary Android SDK**, **Fresco**, **PhotoView** — image loading, direct-to-cloud upload, and pinch-to-zoom recipe photo viewing
- **cooksync-DTOs** — shared DTOs, resolved from `mavenLocal()` (see `settings.gradle.kts`)

## Package layout (`com.cooksync.app`)

| Package | Contents |
|---|---|
| `data/datasource/remote` | `ApiService` (Retrofit interface), `RetrofitClient`, `AuthInterceptor`, `TokenAuthenticator` |
| `data/datasource/local` | `TokenStore` (encrypted session storage), `RecipeDraftStore`, `CookingPreferencesStore` |
| `data/repository` (+ `impl`) | One repository per domain (`Auth`, `Recipe`, `Tag`, `Unit`, `Media`, `Admin`) behind interfaces, implemented on top of `ApiService` |
| `data/model` | Local-only models not covered by the shared DTOs, e.g. `recipe.RecipeDraft` and its mapper/validator/media helper for the recipe wizard |
| `data/service` | `RecipePublishManager` — orchestrates publishing a multi-step recipe draft |
| `domain` | Domain-level helpers/use-cases |
| `ui/base`, `ui/common` | Shared base `Activity`/`Fragment`/`ViewModel` classes and reusable UI components |
| `ui/auth` | Login, registration + OTP verification, forgot/reset password |
| `ui/home` | Home feed / recipe browsing entry point |
| `ui/recipe` | `search`, `detail`, `cooking` (guided step-by-step Cooking Mode with timers), `review`, `favorites`, `myrecipes`, `wizard` (4-step recipe creation), `common` |
| `ui/settings` | Profile, avatar, email/password change, privacy settings, account deactivation/deletion |
| `ui/admin` | Moderation console: users, reported reviews, tag management |
| `util` | Cross-cutting helpers |

Resources are split by feature under `src/main/res` and `src/main/res-features/{auth,home,recipe-*,common,admin,settings}`, wired together via `sourceSets` in `app/build.gradle.kts`.

## Configuration

`BASE_URL` is a `buildConfigField` in `app/build.gradle.kts` (defaults to `http://10.0.2.2:8080/`, the Android emulator's alias for the host machine's `localhost`). Point it at your server:

- **Emulator:** leave as `http://10.0.2.2:8080/`
- **Physical device:** set it to your machine's LAN IP, e.g. `http://192.168.1.x:8080/`

## Running locally

1. Start [`cook-sync-server`](../cook-sync-server) first (see its README).
2. Build and install the shared DTOs module if you haven't already:
   ```bash
   cd ../cooksync-DTOs && mvn install
   ```
3. Open this directory in Android Studio, confirm `BASE_URL` points at your server, and run the app on an emulator or device (`minSdk 24`, `targetSdk`/`compileSdk 36`).

## Tests

```bash
./gradlew test               # JVM unit tests — src/test
./gradlew connectedAndroidTest # Instrumented tests — src/androidTest
```
