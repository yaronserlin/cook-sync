pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Required for com.github.chrisbanes:PhotoView, genuinely hosted on JitPack
        // (unrelated to the shared DTOs, which now resolve from mavenLocal() below).
        maven("https://jitpack.io")
        // Resolves the shared ../cooksync-DTOs module from the local Maven repository
        // (run `mvn install` there after changing a DTO). Both this client and
        // cook-sync-server depend on the exact same DTO sources through this.
        mavenLocal()
    }
}

rootProject.name = "CookSync"
include(":app")
