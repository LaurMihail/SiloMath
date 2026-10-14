plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.silomath.core.testing"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.junit)
    implementation(libs.mockk)
    implementation(libs.kotlinx.coroutines.test)
}
