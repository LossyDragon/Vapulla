// https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint
// https://github.com/google/ksp/releases
plugins {
    id("com.android.application")
    id("com.google.devtools.ksp") version "1.9.0-1.0.12"
    id("org.jlleitschuh.gradle.ktlint") version "11.5.0"
    id("com.google.dagger.hilt.android")
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 34

    defaultConfig {
        namespace = "in.dragonbra.vapulla"
        applicationId = "in.dragonbra.vapulla"

        minSdk = 24
        targetSdk = 34

        versionCode = 4
        versionName = "1.0.0"

        multiDexEnabled = true

        val apiKey = project.properties["steamApiKey"] as String
        buildConfigField("String", "STEAM_API_KEY", apiKey)

        // Supposedly this can break build caching. People say to use a task to copy/rename it.
        // applicationVariants.configureEach { variant ->
        //     variant.outputs.configureEach { output ->
        //         outputFileName = "vapulla-${variant.versionName}.apk"
        //     }
        // }

        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    configurations {
        debugImplementation {
            exclude(group = "junit", module = "junit")
        }
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    composeOptions {
        // https://developer.android.com/jetpack/androidx/releases/compose#declaring_dependencies
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,*.kotlin_module,DEPENDENCIES,LICENSE,NOTICE}"
            excludes += "META-INF/versions/9/previous-compilation-data.bin"
        }
    }
    kapt {
        correctErrorTypes = true
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0")

    // Jetpack Compose:
    // https://developer.android.com/jetpack/androidx/releases/compose
    val composeBom = platform("androidx.compose:compose-bom:2023.06.01")
    implementation(composeBom)
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Accompanist:
    // https://mvnrepository.com/artifact/com.google.accompanist/accompanist-systemuicontroller
    val accompanist = "0.31.6-rc"
    implementation("com.google.accompanist:accompanist-permissions:$accompanist")
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanist")

    // Coil-Kt:
    // https://mvnrepository.com/artifact/com.github.skydoves/landscapist-coil
    implementation("com.github.skydoves:landscapist-coil:2.2.3")

    // Preferences:
    // https://mvnrepository.com/artifact/com.github.alorma/compose-settings-storage-preferences
    val settings = "0.27.0"
    implementation("com.github.alorma:compose-settings-storage-preferences:$settings")
    implementation("com.github.alorma:compose-settings-ui-m3:$settings")

    // Android Support Libs
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")
    implementation("androidx.paging:paging-compose:3.2.0")
    implementation("androidx.paging:paging-runtime-ktx:3.2.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.room:room-paging:2.5.2")

    // Room:
    // https://mvnrepository.com/artifact/androidx.room/room-runtime
    val room = "2.5.2"
    implementation("androidx.room:room-ktx:$room")
    implementation("androidx.room:room-runtime:$room")
    ksp("androidx.room:room-compiler:$room")

    // Google Dagger/Hilt:
    // https://mvnrepository.com/artifact/androidx.hilt/hilt-compiler
    // https://mvnrepository.com/artifact/com.google.dagger/hilt-android
    val hilt = "2.46.1"
    implementation("com.google.dagger:hilt-android:$hilt")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
    kapt("com.google.dagger:hilt-compiler:$hilt")

    // APNG:
    // https://mvnrepository.com/artifact/com.github.penfeizhou.android.animation/apng
    implementation("com.github.penfeizhou.android.animation:apng:2.25.0")

    // OkHttp:
    // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")

    // Retrofit:
    // https://mvnrepository.com/artifact/com.squareup.retrofit2/retrofit
    val retrofit = "2.9.0"
    implementation("com.squareup.retrofit2:converter-gson:$retrofit")
    implementation("com.squareup.retrofit2:retrofit:$retrofit")

    // Jsoup:
    // https://mvnrepository.com/artifact/org.jsoup/jsoup
    implementation("org.jsoup:jsoup:1.16.1")

    // Timber:
    // https://mvnrepository.com/artifact/com.jakewharton.timber/timber
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Java-Steam:
    // https://mvnrepository.com/artifact/in.dragonbra/javasteam
    implementation("in.dragonbra:javasteam:1.3.0") {
        isChanging = version?.contains("SNAPSHOT") ?: false
    }
    // https://mvnrepository.com/artifact/com.madgag.spongycastle/prov
    implementation("com.madgag.spongycastle:prov:1.58.0.0")
    // https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java
    implementation("com.google.protobuf:protobuf-java:3.23.4") // For protobuf builders

    // QR:
    // https://mvnrepository.com/artifact/io.github.g0dkar/qrcode-kotlin
    implementation("io.github.g0dkar:qrcode-kotlin:3.3.0")

    // LeakCanary:
    // https://mvnrepository.com/artifact/com.squareup.leakcanary/leakcanary-android
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}

/**
 * Ktlint gradle
 *  Usages:
 *      gradlew ktlintCheck
 *      gradlew ktlintFormat
 */
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    android.set(true)
    outputToConsole.set(true)
}
