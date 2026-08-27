# CookSync DTOs

Shared Data Transfer Object definitions for the CookSync platform, consumed by both:

- **cook-sync-server** (Spring Boot, Maven)
- **cook-sync-client** (Android, Gradle)

Both live alongside this module in the same repository and consume it as a locally-built
Maven artifact (`com.cooksync:dtos`) from `~/.m2/repository` — no external hosting or
network dependency required.

Keeping these classes in one module guarantees the client and server can never drift on a
payload shape — a compile-time guarantee that hand-duplicated DTOs on each side wouldn't give you.

## Package layout (`com.dtos`)

| Package | Contents |
|---|---|
| `request/auth` | Login, register (+ OTP verify/resend), profile/avatar/email/password update, privacy settings, forgot/reset password, delete account |
| `request/recipe` | Recipe create, visibility update |
| `request/ingredient`, `request/instruction` | Add/edit ingredients and instructions on a recipe |
| `request/review` | Submit a review, report a review |
| `request/note` | Personal instruction notes |
| `request/tags`, `request/unit` | Create/merge tags, create measurement units |
| `response` | `ApiResponse` / `PagedResponse` — generic envelopes used across the API |
| `response/auth` | `AuthResponse`, `PendingRegistrationResponse` |
| `response/recipe` | `RecipeResponse`, `RecipePreviewResponse`, `DescriptionBlockDTO` |
| `response/user` | `UserResponse`, `PublicUserProfileResponse` |
| `response/review`, `response/tags`, `response/unit`, `response/ingredient`, `response/instruction`, `response/note` | One response type per domain |
| `response/admin` | Moderation console payloads: stats, reported reviews, duplicate-tag detection |
| `response/cloudinary` | `CloudinarySignatureResponse` for signed direct uploads |
| `response/errors` | `ApiErrorResponse` |
| `validation` | Custom Jakarta Bean Validation annotations: `@StrongPassword`, `@CurrentPassword`, `@OtpCode` |

## Usage

Build and install this module to your local Maven repository whenever a DTO changes:

```bash
cd cooksync-DTOs
mvn install
```

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

Both consumers pin to the same `1.0.0-SNAPSHOT` version (see `cook-sync-server/pom.xml`'s
`cooksync-dtos.version` property and `cook-sync-client/gradle/libs.versions.toml`'s
`cooksyncDtos` version). After changing a DTO, run `mvn install` here, then rebuild the
server and client so they pick up the refreshed jar from the local Maven repository.
