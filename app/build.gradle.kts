import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val stableDebugKeystore = rootProject.file("debug.keystore")
val stableDebugKeystoreB64 = rootProject.file("debug.keystore.b64")
if (!stableDebugKeystore.exists() && stableDebugKeystoreB64.exists()) {
    stableDebugKeystore.writeBytes(
        Base64.getDecoder().decode(stableDebugKeystoreB64.readText().trim())
    )
}

val playKeystorePath = System.getenv("PARENTBELL_UPLOAD_KEYSTORE")
val playStorePassword = System.getenv("PARENTBELL_UPLOAD_STORE_PASSWORD")
val playKeyAlias = System.getenv("PARENTBELL_UPLOAD_KEY_ALIAS")
val playKeyPassword = System.getenv("PARENTBELL_UPLOAD_KEY_PASSWORD")
val playSigningReady = listOf(
    playKeystorePath,
    playStorePassword,
    playKeyAlias,
    playKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "ca.creativepixels.schoolstuff"
    compileSdk = 36

    defaultConfig {
        applicationId = "ca.creativepixels.schoolstuff"
        minSdk = 26
        targetSdk = 36
        versionCode = 29
        versionName = "0.1.9-internal-test"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    kotlinOptions { jvmTarget = "17" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        getByName("debug") {
            storeFile = stableDebugKeystore
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (playSigningReady) {
            create("playRelease") {
                storeFile = file(playKeystorePath!!)
                storePassword = playStorePassword
                keyAlias = playKeyAlias
                keyPassword = playKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = false
            if (playSigningReady) {
                signingConfig = signingConfigs.getByName("playRelease")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    testImplementation("junit:junit:4.13.2")
}
