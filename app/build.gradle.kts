plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "fr.tbrun.djkids"
    compileSdk = 35
    buildToolsVersion = "35.0.1"

    defaultConfig {
        applicationId = "fr.tbrun.djkids"
        minSdk = 26
        targetSdk = 35
        // Surchargés par Codemagic (BUILD_NUMBER) pour les builds signés.
        versionCode = (System.getenv("DJKIDS_VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("DJKIDS_VERSION_NAME") ?: "1.0.0"
    }

    signingConfigs {
        // Clé de release optionnelle : activée seulement si les variables d'env existent
        // (fournies par Codemagic). Sinon on build en debug.
        val ksPath = System.getenv("DJKIDS_KEYSTORE_PATH")
        if (ksPath != null) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = System.getenv("DJKIDS_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("DJKIDS_KEY_ALIAS")
                keyPassword = System.getenv("DJKIDS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (System.getenv("DJKIDS_KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // media3 expose ses APIs audio bas niveau (AudioProcessor, DefaultAudioSink…) en @UnstableApi.
        freeCompilerArgs += "-opt-in=androidx.media3.common.util.UnstableApi"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Audio : media3 / ExoPlayer pour la lecture, cue, pitch/tempo, volume.
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-common:1.5.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
