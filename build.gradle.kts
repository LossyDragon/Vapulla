// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.ksp) apply false
    alias(libs.plugins.kotlinter) apply false
}

configurations.configureEach {
    resolutionStrategy.cacheChangingModulesFor(1, TimeUnit.SECONDS)
    resolutionStrategy.cacheDynamicVersionsFor(1, TimeUnit.SECONDS)
}