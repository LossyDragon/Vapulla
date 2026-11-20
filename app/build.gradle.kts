import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.stability.analyzer)
}

configurations.configureEach {
    resolutionStrategy.cacheChangingModulesFor(1, TimeUnit.SECONDS)
    resolutionStrategy.cacheDynamicVersionsFor(1, TimeUnit.SECONDS)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "in.dragonbra.vapulla"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "in.dragonbra.vapulla"
        minSdk = 28
        targetSdk = 37

        versionCode = 7
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val releaseKeystore: String? = System.getenv("VAPULLA_KEYSTORE_FILE")

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("VAPULLA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("VAPULLA_KEY_ALIAS")
                keyPassword = System.getenv("VAPULLA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (releaseKeystore != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        // The release build, signed with the debug key so that it installs over a debug build
        // and keeps its data. For measuring speed on a device: a debuggable build runs slower.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    // JavaSteam
    implementation(libs.bundles.javasteam) {
        libs.javasteam.get().let {
            if (it.version?.contains("SNAPSHOT") == true) {
                isChanging = true
            }
        }
    }

    // On Android the Zstandard bindings come as an aar, which carries the native libraries.
    implementation(libs.zstd) {
        artifact { type = "aar" }
    }

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation
    implementation(libs.bundles.navigation)

    // Images & Media
    implementation(libs.bundles.images)

    // Dependency Injection
    implementation(libs.bundles.koin)

    // Database & Storage
    implementation(libs.bundles.database)
    ksp(libs.room.compiler)

    // Utilities
    implementation(libs.android.timber)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.swipe)

    // Unit tests of the plain Kotlin parts; run with ./gradlew :app:testDebugUnitTest
    testImplementation(libs.junit)
}


