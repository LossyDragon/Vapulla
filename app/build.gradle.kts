// https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint
// https://github.com/google/ksp/releases
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.dagger.hilt)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    id("kotlin-parcelize")
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

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true"
                )
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
        kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtensionVersion.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,*.kotlin_module,DEPENDENCIES,LICENSE,NOTICE}"
            excludes += "META-INF/versions/9/previous-compilation-data.bin"
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib.jdk8)

    implementation(libs.javaSteam) {
        isChanging = version?.contains("SNAPSHOT") ?: false
    }

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.leakcanary)

    ksp(libs.bundles.compiler)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.bundles.compose.utils)

    implementation("androidx.compose.ui:ui-text-google-fonts:1.5.4")

    implementation(libs.apng)
    implementation(libs.bundles.retrofit)
    implementation(libs.bundles.room)
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.hilt.android)
    implementation(libs.jsoup)
    implementation(libs.okhttp)
    implementation(libs.paging.compose)
    implementation(libs.paging.runtime.ktx)
    implementation(libs.palette.ktx)
    implementation(libs.preference.ktx)
    implementation(libs.protoBufJava)
    implementation(libs.qrCodeKotlin)
    implementation(libs.spongyCastleProv)
    implementation(libs.timber)
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
