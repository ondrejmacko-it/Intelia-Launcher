import org.jetbrains.kotlin.gradle.dsl.JvmTarget

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val releaseSigning: Properties? = rootProject.file(".signing/release.properties").takeIf { it.isFile }
    ?.let { f -> Properties().apply { f.inputStream().use(::load) } }

android {
    namespace = "dev.intelia.launcher"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "dev.intelia.launcher"
        minSdk = 26
        targetSdk = 37
        // 0.1.0 (D26): first stable release, replaces 0.1.0-beta.1. versionCode only ever goes up.
        versionCode = 3
        versionName = "0.1.0"
    }

    signingConfigs {
        // Lab/debug key (D27): kept only on the owner's PC in `.signing/debug.keystore` (git-ignored). Without it
        // Gradle signs debug builds with this PC's own Android debug key. It never signs a distributed build.
        val labKey = rootProject.file(".signing/debug.keystore")
        if (labKey.isFile) {
            getByName("debug") {
                storeFile = labKey
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        // Production key (D26): only from `.signing/release.properties` on the owner's PC (git-ignored), never the
        // repo's debug key. Without that file `assembleRelease` stays unsigned and cannot be published by mistake.
        if (releaseSigning != null) {
            create("release") {
                storeFile = rootProject.file(releaseSigning.getProperty("storeFile"))
                storePassword = releaseSigning.getProperty("storePassword")
                keyAlias = releaseSigning.getProperty("keyAlias")
                keyPassword = releaseSigning.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Own package "Intelia Debug" (D30): installs next to the owner's Lab, never over it.
            applicationIdSuffix = ".test"
            versionNameSuffix = "-dev"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (releaseSigning != null) signingConfig = signingConfigs.getByName("release")
        }
        // The everyday build on the test phone (D16): release code (R8, not debuggable, AOT from the baseline
        // profile) under the Lab package and the Lab debug key, so `install -r` swaps it with debug both ways.
        create("lab") {
            initWith(getByName("release"))
            applicationIdSuffix = ".lab"
            versionNameSuffix = "-lab"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.animation)
    implementation(libs.compose.material3)
    // Installs the baseline profile on sideloaded builds, so ART compiles the startup path ahead of time (D16).
    implementation(libs.androidx.profileinstaller)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
