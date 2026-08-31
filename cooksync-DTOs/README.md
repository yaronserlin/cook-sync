# cooksync-DTOs

Shared Data Transfer Object definitions for the CookSync platform (**Java 17**, packaged as a plain Maven jar — `com.cooksync:dtos:1.0.0-SNAPSHOT`), consumed by both:

- [**cook-sync-server**](../cook-sync-server) (Spring Boot, Maven)
- [**cook-sync-client**](../cook-sync-client) (Android, Gradle)

This file is self-contained: it covers everything needed to build, install, and consume this module on its own.

Both live alongside this module in the same repository and consume it as a locally-built Maven artifact from `~/.m2/repository` — no external hosting or network dependency required.

Keeping these classes in one module guarantees the client and server can never drift on a payload shape — a compile-time guarantee that hand-duplicated DTOs on each side wouldn't give you.

## Package layout (`com.dtos`)

| Package | Contents |
|---|---|
| `request/auth` | Login, register (+ OTP verify/resend), profile/avatar/email/password update, privacy settings, forgot/reset password, delete account |
| `request/recipe` | Recipe create, visibility update |
| `request/ingredient`, `request/instruction` | Add/edit ingredients and instructions on a recipe |
| `request/review` | Submit a review, report a review |
| `request/note` | Personal instruction notes |
| `request/tags`, `request/unit` | Create/merge tags, create measurement units |
| `response` | `ApiResponse` / `PagedResponse` — generic envelopes used across the whole API |
| `response/auth` | `AuthResponse`, `PendingRegistrationResponse` |
| `response/recipe` | `RecipeResponse`, `RecipePreviewResponse`, `DescriptionBlockDTO` |
| `response/user` | `UserResponse`, `PublicUserProfileResponse` |
| `response/review`, `response/tags`, `response/unit`, `response/ingredient`, `response/instruction`, `response/note` | One response type per domain |
| `response/admin` | Moderation console payloads: stats, reported reviews, duplicate-tag detection |
| `response/cloudinary` | `CloudinarySignatureResponse` for signed direct uploads |
| `response/errors` | `ApiErrorResponse` — the uniform error shape `GlobalExceptionHandler` returns on the server |
| `validation` | Custom Jakarta Bean Validation annotations, composed from Jakarta's built-ins where possible: `@StrongPassword`, `@NewPassword` (adapts `@StrongPassword` for the "new password" field name), `@CurrentPassword`, `@ValidEmail`, `@OtpCode` |

## Usage

Build and install this module to your local Maven repository whenever a DTO changes:

```bash
cd cooksync-DTOs
mvn install
```

Verified working: this installs `dtos-1.0.0-SNAPSHOT.jar` to `~/.m2/repository/com/cooksync/dtos/1.0.0-SNAPSHOT/`, and both consumers below pick it up successfully.

**Maven (server)**, in `pom.xml`:
```xml
<dependency>
    <groupId>com.cooksync</groupId>
    <artifactId>dtos</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Gradle (client)**, in `settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositories {
        mavenLocal()
    }
}
```
and in `app/build.gradle.kts`:
```kotlin
implementation("com.cooksync:dtos:1.0.0-SNAPSHOT")
```

## Versioning

Both consumers pin to the same `1.0.0-SNAPSHOT` version (see `cook-sync-server/pom.xml`'s `cooksync-dtos.version` property and `cook-sync-client/gradle/libs.versions.toml`'s `cooksyncDtos` version). After changing a DTO, run `mvn install` here, then rebuild the server and client so they pick up the refreshed jar from the local Maven repository — a stale build will silently keep using the old shape until you do.
