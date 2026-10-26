import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

fun localProperty(name: String): String {
    return localProperties
        .getProperty(name)
        ?.trim()
        .orEmpty()
}

fun buildConfigString(value: String): String {
    return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

fun environmentProperty(
    environment: String,
    name: String,
    developmentFallback: String? = null): String {
    val environmentValue =
        localProperty("${environment.uppercase()}_$name")

    if (environmentValue.isNotEmpty()) {
        return environmentValue
    }

    if (environment == "development" && developmentFallback != null) {
        return localProperty(developmentFallback)
    }

    return ""
}

android {
    namespace = "com.silomath.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.silomath.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "environment"

    productFlavors {
        create("development") {
            dimension = "environment"

            buildConfigField(
                "String",
                "ENVIRONMENT",
                buildConfigString("development")
            )

            buildConfigField(
                "String",
                "API_BASE_URL",
                buildConfigString(
                    environmentProperty(
                        environment = "development",
                        name = "API_BASE_URL",
                        developmentFallback = "API_BASE_URL"
                    )
                )
            )

            buildConfigField(
                "String",
                "SUPABASE_URL",
                buildConfigString(
                    environmentProperty(
                        environment = "development",
                        name = "SUPABASE_URL",
                        developmentFallback = "SUPABASE_URL"
                    )
                )
            )

            buildConfigField(
                "String",
                "SUPABASE_PUBLISHABLE_KEY",
                buildConfigString(
                    environmentProperty(
                        environment = "development",
                        name = "SUPABASE_PUBLISHABLE_KEY",
                        developmentFallback = "SUPABASE_PUBLISHABLE_KEY"
                    )
                )
            )
        }

        create("staging") {
            dimension = "environment"

            buildConfigField(
                "String",
                "ENVIRONMENT",
                buildConfigString("staging")
            )

            buildConfigField(
                "String",
                "API_BASE_URL",
                buildConfigString(
                    environmentProperty(
                        environment = "staging",
                        name = "API_BASE_URL"
                    )
                )
            )

            buildConfigField(
                "String",
                "SUPABASE_URL",
                buildConfigString(
                    environmentProperty(
                        environment = "staging",
                        name = "SUPABASE_URL"
                    )
                )
            )

            buildConfigField(
                "String",
                "SUPABASE_PUBLISHABLE_KEY",
                buildConfigString(
                    environmentProperty(
                        environment = "staging",
                        name = "SUPABASE_PUBLISHABLE_KEY"

                    )
                )
            )
        }

        create("production") {
            dimension = "environment"

            buildConfigField(
                "String",
                "ENVIRONMENT",
                buildConfigString("production")
            )

            buildConfigField(
                "String",
                "API_BASE_URL",
                buildConfigString(
                    environmentProperty(
                        environment = "production",
                        name = "API_BASE_URL"
                    )
                )
            )

            buildConfigField(
                "String",
                "SUPABASE_URL",
                buildConfigString(
                    environmentProperty(
                        environment = "production",
                        name = "SUPABASE_URL"
                    )
                )
            )

            buildConfigField(
                "String",
                "SUPABASE_PUBLISHABLE_KEY",
                buildConfigString(
                    environmentProperty(
                        environment = "production",
                        name = "SUPABASE_PUBLISHABLE_KEY"
                    )
                )
            )
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("androidTest").assets.srcDirs(
            "$projectDir/schemas"
        )
    }
}

ksp {
    arg(
        "room.schemaLocation",
        "$projectDir/schemas"
    )
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:sync"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:masterdata"))
    implementation(project(":calculation-engine"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.ext.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.okhttp.logging)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.android)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(project(":core:common"))
    testImplementation(project(":core:model"))
    testImplementation(project(":core:domain"))
    testImplementation(project(":core:database"))
    testImplementation(project(":core:network"))
    testImplementation(project(":core:testing"))

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(project(":core:common"))
    androidTestImplementation(project(":core:model"))
    androidTestImplementation(project(":core:domain"))
    androidTestImplementation(project(":core:database"))

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    androidTestImplementation(libs.mockk.android)

    androidTestImplementation(
        libs.roomTesting
    )

    debugImplementation(libs.androidx.compose.ui.tooling)

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )
}
