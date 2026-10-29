import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { stream ->
            load(stream)
        }
    }
}

android {
    namespace = "com.euthopiar.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "SUBDL_API_KEY", "\"${localProperties.getProperty("SUBDL_API_KEY", "")}\"")
        buildConfigField("String", "TRAKT_CLIENT_ID", "\"${localProperties.getProperty("TRAKT_CLIENT_ID", "")}\"")
        buildConfigField("String", "TRAKT_CLIENT_SECRET", "\"${localProperties.getProperty("TRAKT_CLIENT_SECRET", "")}\"")
        buildConfigField("String", "SIMKL_CLIENT_ID", "\"${localProperties.getProperty("SIMKL_CLIENT_ID", "")}\"")
        buildConfigField("String", "TMDB_API_KEY", "\"${localProperties.getProperty("TMDB_API_KEY", "")}\"")
        buildConfigField("String", "FREEKZ_KEY_TOKEN", "\"${localProperties.getProperty("FREEKZ_KEY_TOKEN", "")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xskip-metadata-version-check")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            it.maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(2)
        }
    }
}

dependencies {
    api(files("libs/quickjs-kt.jar"))
    compileOnly(files("libs/lib-nuvio-engine-android-0.1.4.aar"))
    testImplementation(files("libs/lib-nuvio-engine-android-0.1.4.aar"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.jsoup)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.dylibso.chicory:runtime:1.0.0")
    compileOnly(platform(libs.androidx.compose.bom))
    compileOnly("androidx.compose.runtime:runtime")
    testImplementation("junit:junit:4.13.2")
}
