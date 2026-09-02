import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Release signing credentials, read from keystore.properties (gitignored - never committed).
// Both the properties file and the .jks it points at are missing on a fresh clone until you
// generate a release keystore; guarded below so assembleRelease still produces an unsigned
// APK without them instead of failing.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
var hasReleaseKeystore = false
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    val storeFilePath = keystoreProperties.getProperty("storeFile")
    hasReleaseKeystore = storeFilePath != null && rootProject.file(storeFilePath).exists()
}

android {
    namespace = "com.cooksync.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.cooksync.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        resValue("string", "app_name", "CookSync")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // Ensure the APK contains libraries for all common architectures.
            // This fixes "0 split apks compatible" errors during deployment.
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"))
        }
    }

    if (hasReleaseKeystore) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    splits {
        // Explicitly disable APK splits to ensure a single "universal" APK is built,
        // preventing the "none of the 0 split apks are compatible" deployment error.
        abi {
            isEnable = false
        }
    }

    buildTypes {
        debug {
            // Local dev server address. Update to your machine's LAN IP when testing on a
            // physical device instead of the emulator.
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
        }
        release {
            // Production API, deployed on Render.
            buildConfigField("String", "BASE_URL", "\"https://cooksyncapp-server.onrender.com/\"")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    sourceSets {
        getByName("main") {
            res.srcDirs(
                "src/main/res",
                "src/main/res-features/auth",
                "src/main/res-features/home",
                "src/main/res-features/recipe-add",
                "src/main/res-features/recipe-favorites",
                "src/main/res-features/recipe-myrecipes",
                "src/main/res-features/recipe-common",
                "src/main/res-features/common",
                "src/main/res-features/admin",
                "src/main/res-features/settings"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

configurations.all {
    // Several AndroidX libraries (appcompat, constraintlayout, work, ...) pull in
    // profileinstaller transitively. Excluded so the app doesn't carry the runtime
    // library that would otherwise install/compile a baseline profile in the
    // background - see the task-disabling block below for why a profile still
    // ends up embedded regardless of this exclusion.
    exclude(group = "androidx.profileinstaller", module = "profileinstaller")
}

tasks.configureEach {
    // AGP merges "baseline-prof.txt" files bundled inside AndroidX AARs (appcompat,
    // work, lifecycle, ...) into assets/dexopt/baseline.prof on its own, regardless of
    // whether androidx.profileinstaller (excluded above) is even on the classpath to
    // read it - that exclusion alone does not stop the embedding. Android Studio's
    // deployer then tries to install that embedded profile via a companion .dm file
    // at install time, which fails with INSTALL_BASELINE_PROFILE_FAILED
    // (run-from-apk, reason=unknown) on this machine's x86_64 emulator and aborts the
    // whole install. Disabling the tasks that produce/merge the profile stops one
    // from being embedded in the first place.
    if (name.contains("ArtProfile") || name.contains("StartupProfile")) {
        enabled = false
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)
    testImplementation(libs.junit)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    // Shared DTOs (../cooksync-DTOs) — single source of truth for request/response
    // payload shapes, consumed identically by cook-sync-server (Maven) from the
    // local Maven repository.
    implementation(libs.cooksync.dtos)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    implementation(libs.security.crypto)

    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    implementation(libs.glide)
    implementation(libs.cloudinary.android)
    implementation(libs.fresco)
    implementation(libs.photoview)
}
