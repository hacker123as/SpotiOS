import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization")
}

val keystorePropertiesFile = rootProject.file("keystore/keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}


android {
    namespace = "com.project.lol"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.spotios.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 270
        versionName = "2.7.0"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    val envStore = System.getenv("SPOTIOS_KEYSTORE")
    val hasPropsKey = keystorePropertiesFile.exists()
    val hasEnvKey = !envStore.isNullOrBlank() && file(envStore).exists()

    signingConfigs {
        if (hasPropsKey || hasEnvKey) {
            create("release") {
                if (hasEnvKey) {
                    storeFile = file(envStore!!)
                    storePassword = System.getenv("SPOTIOS_STORE_PASSWORD")
                    keyAlias = System.getenv("SPOTIOS_KEY_ALIAS")
                    keyPassword = System.getenv("SPOTIOS_KEY_PASSWORD")
                } else {
                    storeFile = rootProject.file("keystore/${keystoreProperties.getProperty("storeFile")}")
                    storePassword = keystoreProperties.getProperty("storePassword")
                    keyAlias = keystoreProperties.getProperty("keyAlias")
                    keyPassword = keystoreProperties.getProperty("keyPassword")
                }
            }
        }
    }

    buildTypes {
        debug {
        }
        release {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.media)
    implementation(libs.bouncyprov)
    implementation(libs.bouncypkix)
    implementation(libs.security.crypto)


    // Jetpack Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.tabler.icons)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    debugImplementation(libs.compose.ui.tooling)

    // Glance (home screen widgets)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Ktor + serialization (YouTube InnerTube client)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.serialization.json)
    implementation(libs.serialization.json)

    // NewPipe + YouTube streaming
    implementation(libs.newpipeextractor)
    implementation(libs.brotli)
    implementation(libs.okhttp)

    // Audio downloads: opus decoding, mp3 encoding
    implementation(project(":lame"))
    implementation(project(":opus"))

    // Core library desugaring (required by NewPipeExtractor)
    coreLibraryDesugaring(libs.desugaring)
}