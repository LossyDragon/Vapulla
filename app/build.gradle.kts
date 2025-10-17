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

        // kapt {
        //     arguments {
        //         arg("room.schemaLocation", "$projectDir/schemas".toString())
        //     }
        // }

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

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // JavaSteam
    implementation("in.dragonbra:javasteam:1.8.0-SNAPSHOT")
    
    implementation(libs.android.timber)
    implementation(libs.nav3.runtime)
    implementation(libs.nav3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.nav3)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material3.windowsizeclass)
    implementation(libs.androidx.material3.adaptive)
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation("androidx.preference:preference:1.2.1") // TODO Remove
    implementation("com.google.protobuf:protobuf-java:4.32.1")
    implementation("com.squareup.okhttp3:okhttp:5.2.1")
    implementation("com.github.skydoves:landscapist-coil:2.6.1")
    implementation("com.github.penfeizhou.android.animation:apng:3.0.5")
    implementation("io.github.alexzhirkevich:qrose:1.0.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.82")
    implementation("androidx.room:room-runtime:2.8.2")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("androidx.room:room-ktx:2.8.2")
    implementation("androidx.room:room-paging:2.8.2")
    implementation("androidx.paging:paging-runtime:3.3.6")
    ksp("androidx.room:room-compiler:2.8.2")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}


