import com.android.sdklib.AndroidVersion.VersionCodes
import jdk.internal.jshell.debug.InternalDebugControl.release
import org.gradle.kotlin.dsl.detektPlugins
import java.lang.module.ModuleFinder.compose
import java.time.LocalDateTime
import java.time.ZoneId

plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.hilt)
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

    implementation(libs.android.hilt.android)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.android.hilt.compiler)

    implementation(libs.room)
    ksp(libs.room.compiler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.register("generateBuildRecord") {
    description = "Generate a build record with the current build time, commit ID, and version."
    group = "build"
    doLast {
        val buildTime = LocalDateTime.now(ZoneId.of("UTC")).toString()
        val commitId = getCheckedOutGitCommitHash()
        val buildRecord =
            """
            buildTime=$buildTime
            commitId=$commitId
            version=${android.defaultConfig.versionName}
            """.trimIndent()
        val file = file("src/main/res/raw/build_record.properties")
        file.parentFile.mkdirs()
        file.writeText(buildRecord)
    }
}

// https://gist.github.com/JonasGroeger/7620911
private fun getCheckedOutGitCommitHash(): String {
    val gitFolder = "${rootProject.projectDir}/../.git"
    val takeFromHash = 12

    // '.git/HEAD' contains either
    //      in case of detached head: the currently checked out commit hash
    //      otherwise: a reference to a file containing the current commit hash
    val head = File(gitFolder, "HEAD").readText().split(":") // .git/HEAD
    val isCommit = head.size == 1 // e5a7c79edabbf7dd39888442df081b1c9d8e88fd
    // val isRef = head.size > 1     // ref: refs/heads/master

    return if (isCommit) {
        head[0].trim().take(takeFromHash) // e5a7c79edabb
    } else {
        val refHead = File(gitFolder, head[1].trim()) // .git/refs/heads/master
        refHead.readText().trim().take(takeFromHash)
    }
}
