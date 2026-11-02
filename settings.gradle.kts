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
        google()
        mavenCentral()
    }
}

rootProject.name = "SiloMath"
include(
    ":app",
    ":core:model",
    ":core:common",
    ":core:domain",
    ":core:database",
    ":core:network",
    ":core:sync",
    ":core:designsystem",
    ":core:navigation",
    ":core:testing",
    ":feature:auth",
    ":feature:dashboard",
    ":feature:masterdata",
    ":feature:campaigns",
    ":feature:stocktaking",
    ":feature:close",
    ":feature:reports",
    ":feature:administration",
    ":calculation-engine"
)
