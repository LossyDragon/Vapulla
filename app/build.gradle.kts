import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "in.dragonbra.vapulla"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "in.dragonbra.vapulla"
        minSdk = 36
        targetSdk = 36

        versionCode = 5
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // applicationVariants.all { variant ->
        //     variant.outputs.all { output ->
        //         outputFileName = "vapulla-${variant.versionName}.apk"
        //     }
        // }
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
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
}


