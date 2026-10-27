pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
            // Preserve the backend's existing runtime version; no transitive serialization upgrade.
            version("kotlinxSerialization", "1.9.0")
        }
    }
}

rootProject.name = "silomath-backend"
include(":calculation-engine")
project(":calculation-engine").projectDir = file("../calculation-engine")
