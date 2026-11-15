pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // A JavaSteam build in Maven local (~/.m2) wins over the published snapshot, to try a
        // library change before publishing it. Delete ~/.m2/repository/in/dragonbra to go
        // back to the published one.
        mavenLocal {
            content { includeGroup("in.dragonbra") }
        }
        google()
        mavenCentral()
        maven(url = "https://central.sonatype.com/repository/maven-snapshots/")
    }
}

rootProject.name = "Vapulla"
include(":app")
