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
val appVersionCode = 56
val appVersionName = "0.19.0"

if (hasReleaseKeystore) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

val releaseKeyAlias =
    providers.environmentVariable("RELEASE_KEY_ALIAS").orNull ?: keystoreProperties.getProperty("KEY_ALIAS")
val releaseKeyPassword =
    providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull ?: keystoreProperties.getProperty("KEY_PASSWORD")
val releaseStorePassword =
    providers.environmentVariable("RELEASE_STORE_PASSWORD").orNull ?: keystoreProperties.getProperty("KEYSTORE_PASSWORD")
val releaseKeystorePath =
    providers.environmentVariable("RELEASE_KEYSTORE_PATH").orNull ?: keystoreProperties.getProperty("KEYSTORE_PATH")
val hasReleaseSigningConfig = listOf(
    releaseKeyAlias,
    releaseKeyPassword,
    releaseStorePassword,
    releaseKeystorePath,
).none { it.isNullOrBlank() }

if (isReleaseTask && !hasReleaseSigningConfig) {
    throw GradleException(
        "Release signing requires RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_STORE_PASSWORD, and " +
            "RELEASE_KEYSTORE_PATH environment variables or equivalent values in keystore.properties",
    )
}

android {
    compileSdk = 37
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "org.secu3.android"
        minSdk = 23
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigningConfig) {
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
                storeFile = file(requireNotNull(releaseKeystorePath))
                storePassword = requireNotNull(releaseStorePassword)
            }
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "retrofit2.pro",
            )
            if (hasReleaseSigningConfig) {
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

tasks.register("validateVersionTag") {
    group = "verification"
    description = "Checks that the GitHub release tag matches the Android version name"

    val releaseTag = providers.environmentVariable("GITHUB_REF_NAME")
    inputs.property("releaseTag", releaseTag.orElse(""))
    inputs.property("versionName", appVersionName)

    doLast {
        val actualTag = releaseTag.orNull
            ?: throw GradleException("GITHUB_REF_NAME is required to validate a release tag")
        val expectedTag = "v$appVersionName"

        if (actualTag != expectedTag) {
            throw GradleException(
                "Release tag '$actualTag' does not match versionName '$appVersionName'. Expected tag: '$expectedTag'",
            )
        }
    }
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
    debugImplementation(libs.compose.ui.test.manifest)
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
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.dagger.hilt.testing)
    kspAndroidTest(libs.dagger.hilt.compiler)
}
