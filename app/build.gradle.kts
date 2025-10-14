import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
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

        val vapullaImgurClientId: String by project
        val vapullaImgurClientSecret: String by project
        buildConfigField("String", "IMGUR_CLIENT_ID", "\"$vapullaImgurClientId\"")
        buildConfigField("String", "IMGUR_CLIENT_SECRET", "\"$vapullaImgurClientSecret\"")
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
       // compose = true
        viewBinding = true
    }
}

//noinspection UseTomlInstead // TODO remove
dependencies {
    // TODO remove
    val kotlin_version = "2.2.20"
    val android_support_version = "28.0.0"
    val glide_version = "5.0.5"
    val retrofit_verson = "3.0.0"
    val dagger_version = "2.57.2"
    val room_version = "2.8.2"

    // implementation fileTree (dir: "libs", include: ["*.jar"])
    // implementation("org.jetbrains.anko:anko:0.10.8")
    androidTestImplementation("com.android.support.test.espresso:espresso-core:3.0.2")
    androidTestImplementation("com.android.support.test:runner:1.0.2")
    implementation("android.arch.lifecycle:livedata:1.1.1")
    implementation("android.arch.paging:runtime:1.0.1")
    implementation("androidx.room:room-runtime:${room_version}")
    implementation("com.android.support.constraint:constraint-layout:2.0.4")
    implementation("com.android.support:appcompat-v7:$android_support_version")
    implementation("com.android.support:design:$android_support_version")
    implementation("com.android.support:recyclerview-v7:$android_support_version")
    implementation("com.android.support:support-annotations:$android_support_version")
    implementation("com.android.support:support-v4:$android_support_version")
    implementation("com.brandongogetap:stickyheaders:0.6.2")
    implementation("com.github.bumptech.glide:glide:$glide_version")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.google.dagger:dagger:$dagger_version")
    implementation("com.hannesdorfmann.mosby3:mvp:3.1.1")
    implementation("com.madgag.spongycastle:prov:1.58.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.2.1")
    implementation("com.google.protobuf:protobuf-java:4.32.1")
    implementation("com.squareup.retrofit2:converter-gson:$retrofit_verson")
    implementation("com.squareup.retrofit2:retrofit:$retrofit_verson")
    implementation("in.dragonbra:javasteam:1.8.0-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    ksp("androidx.room:room-compiler:$room_version")
    ksp("com.github.bumptech.glide:compiler:$glide_version")
    ksp("com.google.dagger:dagger-compiler:$dagger_version")
    testImplementation("junit:junit:4.13.2")
}


