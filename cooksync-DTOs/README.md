# CookSync DTOs

Shared Data Transfer Object definitions for the CookSync platform, consumed by both:

- **cook-sync-server** (Spring Boot, Maven)
- **cook-sync-client** (Android, Gradle)

Both live alongside this module in the same repository and consume it as a locally-built
Maven artifact (`com.cooksync:dtos`) from `~/.m2/repository` — no external hosting or
network dependency required.

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
