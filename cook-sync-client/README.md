# cook-sync-client

The CookSync Android app (**Java 17**, MVVM, `minSdk 24` / `targetSdk` & `compileSdk 36`). It talks to [`cook-sync-server`](../cook-sync-server) over REST and shares its request/response payload shapes with it via the [`cooksync-DTOs`](../cooksync-DTOs) module.

This file is self-contained: it covers everything needed to configure and run this module on its own.

## Tech stack

- **MVVM** — `ViewModel` + `LiveData`, one package per feature under `ui/`, with every `Repository` call wrapped in a 3-state `ApiResult<T>` (`Loading`/`Success`/`Error`) so every screen handles results the same way
- **Retrofit** + **OkHttp** + **Gson** — networking; an `AuthInterceptor` attaches the `Authorization: Bearer <token>` header, and a `TokenAuthenticator` transparently refreshes an expired access token and retries the request on a 401
- **androidx.security (`security-crypto`)** — encrypted on-device storage for the session (access + refresh tokens), via `TokenStore`
- **Glide**, **Cloudinary Android SDK**, **Fresco**, **PhotoView** — image loading, direct-to-cloud photo upload (the app uploads straight to Cloudinary; only the resulting URL is sent to the CookSync server), and pinch-to-zoom recipe photo viewing
- **cooksync-DTOs** — shared request/response DTOs, resolved from `mavenLocal()` (see `settings.gradle.kts`)

## Package layout (`com.cooksync.app`)

| Package | Contents |
|---|---|
| `data/datasource/remote` | `ApiService` (Retrofit interface), `RetrofitClient` (builds it once), `AuthInterceptor`, `TokenAuthenticator` |
| `data/datasource/local` | `TokenStore` (encrypted session storage), `RecipeDraftStore` (local-only recipe drafts, not synced across devices), `CookingPreferencesStore` |
| `data/repository` (+ `impl`) | One repository per domain — `Auth`, `Recipe`, `Tag`, `Unit`, `Media`, `Admin` — behind interfaces, implemented on top of `ApiService`; `BaseRepository` holds shared request-execution helpers |
| `data/model` | Local-only models not covered by the shared DTOs, e.g. `recipe.RecipeDraft` and its mapper/validator/media helper for the recipe wizard |
| `data/service` | `RecipePublishManager` — a process-wide singleton that publishes a recipe draft (image upload → new-tag creation → save) on a dedicated background thread, independent of any screen's lifecycle, so publishing survives navigation |
| `domain` | `ApiResult<T>` and other domain-level helpers |
| `ui/base`, `ui/common` | Shared base classes (`BaseActivity`, `BaseAdapter`, `BaseViewModel`, `Navigator`, `ViewModelFactory`) and reusable UI components |
| `ui/auth` | Login, registration + OTP verification, forgot/reset password, and the public user-profile screen |
| `ui/home` | Home feed / recipe browsing entry point |
| `ui/recipe` | `search`, `detail`, `cooking` (guided step-by-step Cooking Mode with timers), `review`, `favorites`, `myrecipes`, `wizard` (4-step recipe creation/edit), `common` (shared filter bottom sheet, list-screen base classes) |
| `ui/settings` | Profile, avatar, email/password change, privacy settings, account deactivation/deletion, legal documents |
| `ui/admin` | Moderation console: users, reported reviews, tag merging, units |
| `util` | Cross-cutting helpers — `SessionManager`, `PendingActionScheduler` (the optimistic-update-with-undo mechanism used by favorites/visibility-toggle/admin actions: the UI updates immediately, the network call fires after a short delay unless cancelled), `CommitOnceGuard` (de-dupes a save triggered by two UI events at once, e.g. a save tap and the focus-loss it causes) |

Resources are split by feature under `src/main/res` and `src/main/res-features/{auth,home,recipe-*,common,admin,settings}`, wired together via `sourceSets` in `app/build.gradle.kts`.

## Configuration

`BASE_URL` is a per-build-type `buildConfigField` in `app/build.gradle.kts` — there's no separate "dev"/"prod" flavor; `debug`/`release` already serve that role:

- **`debug`** — `http://10.0.2.2:8080/`, the Android emulator's alias for the host machine's `localhost`. For a physical device on the same Wi-Fi network, change this to your machine's LAN IP, e.g. `http://192.168.1.x:8080/`.
- **`release`** — `https://cooksyncapp-server.on-render.com/`, the production API.

### Release signing

`app/build.gradle.kts` reads signing credentials from `keystore.properties` (repo root of this module, gitignored — never committed) if present, and only wires up `signingConfig` on the `release` build type when both that file and the `.jks` it points at actually exist; otherwise `assembleRelease` still succeeds but produces an unsigned APK. To build a real signed release:

1. Generate a keystore if you don't have one yet: `keytool -genkeypair -v -keystore keystore/cooksync-release.jks -alias <your-key-alias> -keyalg RSA -keysize 2048 -validity 10000` (run from this directory; `keystore/` and `*.jks` are gitignored).
2. Create `keystore.properties` in this directory:
   ```properties
   storeFile=keystore/cooksync-release.jks
   storePassword=<your store password>
   keyAlias=<your-key-alias>
   keyPassword=<your key password>
   ```
3. `./gradlew :app:assembleRelease` now produces a signed, minified (R8) APK.

## Running locally

1. Start [`cook-sync-server`](../cook-sync-server) first (see its README) — either directly via `./mvnw spring-boot:run`, or with `./docker-up.sh` from the repo root.
2. Build and install the shared DTOs module if you haven't already:
   ```bash
   cd ../cooksync-DTOs && mvn install
   ```
3. Open this directory in Android Studio, confirm the `debug` build type's `BASE_URL` points at your server, and run the app on an emulator or device.

Verified working: `./gradlew :app:assembleDebug` and `./gradlew :app:assembleRelease` (R8 minification on, unsigned without a keystore) both build successfully against a freshly-installed `cooksync-DTOs` artifact.

## Tests

```bash
./gradlew test                 # JVM unit tests — src/test (18 test classes)
./gradlew connectedAndroidTest  # Instrumented tests — src/androidTest (requires a running emulator/device)
```
