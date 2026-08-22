# CookSync DTOs

Shared Data Transfer Object definitions for the CookSync platform, consumed by both:

- **cook-sync-server** (Spring Boot, Maven) — `com.github.yaronserlin:cooksync-DTOs:<tag>`
- **cook-sync-client** (Android, Gradle) — `com.github.yaronserlin:cooksync-DTOs:<tag>`

Distributed via [JitPack](https://jitpack.io), which builds this repository directly from
GitHub tags/commits — no manual publishing step. Both consumers resolve the exact same
artifact, so request/response payload shapes never drift between client and server.

## Usage

**Maven (server):**
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.yaronserlin</groupId>
    <artifactId>cooksync-DTOs</artifactId>
    <version>TAG</version>
</dependency>
```

**Gradle (client), in `settings.gradle.kts`:**
```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```
and in `app/build.gradle.kts`:
```kotlin
implementation("com.github.yaronserlin:cooksync-DTOs:TAG")
```

## Versioning

Tag a commit (e.g. `git tag v1.0.0 && git push --tags`) whenever a DTO changes. JitPack
builds that tag on first request and caches it — both server and client should pin to the
same tag to stay in sync.
