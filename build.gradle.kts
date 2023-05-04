// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
}

plugins {
    id("com.android.application") version "8.0.0" apply false
    id("com.android.library") version "8.0.0" apply false
    id("org.jetbrains.kotlin.android") version "1.8.20" apply false
    id("com.google.dagger.hilt.android") version "2.46" apply false
}

// Used to try not to cache JavaSteam snapshots when developing
configurations.configureEach {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}
