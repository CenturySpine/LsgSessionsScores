import java.util.*

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Load Supabase config from local.properties
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
}
val supabaseUrl = (localProps.getProperty("supabase.url") ?: "")
val supabaseAnonKey = (localProps.getProperty("supabase.anonKey") ?: "")
val supabaseBucketPlayers = (localProps.getProperty("supabase.bucket.players") ?: "players")
val supabaseBucketHoles = (localProps.getProperty("supabase.bucket.holes") ?: "holes")
val supabaseBucketSessions = (localProps.getProperty("supabase.bucket.sessions") ?: "sessions")

// Release signing credentials from user/global gradle.properties or environment variables (do not commit secrets)
val releaseStoreFile = (project.findProperty("RELEASE_STORE_FILE") as String?) ?: System.getenv("RELEASE_STORE_FILE")
val releaseStorePassword =
    (project.findProperty("RELEASE_STORE_PASSWORD") as String?) ?: System.getenv("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = (project.findProperty("RELEASE_KEY_ALIAS") as String?) ?: System.getenv("RELEASE_KEY_ALIAS")
val releaseKeyPassword =
    (project.findProperty("RELEASE_KEY_PASSWORD") as String?) ?: System.getenv("RELEASE_KEY_PASSWORD")
val hasReleaseSigning =
    listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() }

android {
    namespace = "fr.centuryspine.lsgscores"
    compileSdk = 36

    defaultConfig {
        applicationId = "fr.centuryspine.lsgscores"
        minSdk = 29
        //noinspection OldTargetApi
        targetSdk = 35
        versionCode = 10
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Expose Supabase config to BuildConfig
        buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supabaseAnonKey}\"")
        buildConfigField("String", "SUPABASE_BUCKET_PLAYERS", "\"${supabaseBucketPlayers}\"")
        buildConfigField("String", "SUPABASE_BUCKET_HOLES", "\"${supabaseBucketHoles}\"")
        buildConfigField("String", "SUPABASE_BUCKET_SESSIONS", "\"${supabaseBucketSessions}\"")
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

    if (hasReleaseSigning) {
        println("[Gradle] Using provided RELEASE_* properties to sign release builds")
        signingConfigs {
            create("release")
        }
        signingConfigs.getByName("release") {
            storeFile = file(releaseStoreFile!!)
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    } else {
        println("[Gradle] Release signing not configured (missing RELEASE_* properties). AAB/APK will be unsigned.")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.material3)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose.v190)
    implementation(libs.androidx.core.ktx.v1131)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.android.image.cropper)
    implementation(libs.coil.compose)
    implementation(libs.accompanist.flowlayout)
    implementation(libs.androidx.material.icons.extended)
    implementation(platform(libs.androidx.compose.bom.v20240500))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    implementation(libs.play.services.location)
    implementation(libs.coil.compose)

    // QR and Scanning
    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    // Supabase and Ktor client
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.okhttp)
}
