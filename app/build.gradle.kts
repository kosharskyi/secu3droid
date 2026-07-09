import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp.plugin)
    id("com.google.gms.google-services")
    alias(libs.plugins.dagger.hilt.plugin)
    alias(libs.plugins.navigation.safeargs)
    id("com.google.firebase.crashlytics")
    alias(libs.plugins.room.plugin)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasReleaseKeystore = keystorePropertiesFile.exists()
val isReleaseTask = gradle.startParameter.taskNames.any { it.lowercase().contains("release") }

if (hasReleaseKeystore) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
} else if (isReleaseTask) {
    throw GradleException("keystore.properties is required for release builds")
}

android {
    compileSdk = 37
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "org.secu3.android"
        minSdk = 23
        targetSdk = 36
        versionCode = 55
        versionName = "0.18.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                keyAlias = keystoreProperties["KEY_ALIAS"] as String?
                keyPassword = keystoreProperties["KEY_PASSWORD"] as String?
                storeFile = file(keystoreProperties["KEYSTORE_PATH"] as String)
                storePassword = keystoreProperties["KEYSTORE_PASSWORD"] as String?
            }
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "retrofit2.pro",
            )
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
//            versionNameSuffix = "-debug" + "-build" + getDate()
            versionNameSuffix = "-debug"
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }

    namespace = "org.secu3.android"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(libs.kotlin.stdlib)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.fragment.ktx)
    implementation(libs.annotation)

    implementation(libs.preference.ktx)
    implementation(libs.constraintlayout)
    implementation(libs.viewpager2)

    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.service)

    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    implementation(libs.androidx.browser)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.material3.android)
    ksp(libs.room.compiler)

    implementation(libs.material)
    implementation(libs.gson)

    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.runtime)
    implementation(libs.compose.viewmodel)
    implementation(libs.compose.livedata)
    implementation(libs.compose.foundation)

    implementation(libs.androidx.activity.compose)

    implementation(libs.threetenabp)

    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.retrofit)

    // Dagger 2
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    implementation(libs.speedviewlib)

    testImplementation(libs.junit)
    testImplementation(libs.room.testing)
    testImplementation(libs.dagger.hilt.testing)
    kspTest(libs.dagger.hilt.compiler)

    implementation(libs.usb.serial.android)

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.dagger.hilt.testing)
    kspAndroidTest(libs.dagger.hilt.compiler)
}
