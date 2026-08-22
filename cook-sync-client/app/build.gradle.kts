plugins {
    alias(libs.plugins.android.application)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
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
        // Local dev server address. Update to your machine's LAN IP when testing on a device.
        buildConfigField("String", "BASE_URL", "\"http://172.20.10.3:8080/\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // Ensure the APK contains libraries for all common architectures.
            // This fixes "0 split apks compatible" errors during deployment.
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"))
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
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    // Security — encrypted storage for JWT access/refresh tokens
    implementation(libs.security.crypto)

    // Lifecycle — MVVM ViewModel + LiveData
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Images
    implementation(libs.glide)
    implementation(libs.cloudinary.android)
    implementation(libs.fresco)
    implementation(libs.photoview)

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}
