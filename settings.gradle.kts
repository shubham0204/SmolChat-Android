import java.net.URI

include(":smolvectordb")


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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = URI("https://jitpack.io") }
        maven { url = URI("https://oss.sonatype.org/content/repositories/snapshots/") }
    }
}

rootProject.name = "SmolChat Android"
include(":app")
include(":smollm")
include(":hf-model-hub-api")
include(":whisper")
