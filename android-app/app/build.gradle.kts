import com.android.sdklib.AndroidVersion.VersionCodes
import jdk.internal.jshell.debug.InternalDebugControl.release
import org.gradle.kotlin.dsl.detektPlugins
import java.lang.module.ModuleFinder.compose

plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.sakurasou.renkei"
    compileSdk {
        version =
            release(VersionCodes.BAKLAVA) {
                minorApiLevel = 1
            }
    }

    defaultConfig {
        applicationId = "io.sakurasou.renkei"
        minSdk = VersionCodes.TIRAMISU
        targetSdk = VersionCodes.BAKLAVA
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    detektPlugins(libs.detekt.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    testImplementation(libs.okhttp.mockwebserver)
    implementation(platform(libs.retrofit.bom))
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)

    implementation(libs.android.hilt.android)
    ksp(libs.android.hilt.compiler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    implementation(libs.androidx.material3.adaptive)
    implementation(libs.androidx.material3.adaptive.layout)
    // implementation(libs.androidx.material3.adaptive.navigation3)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
