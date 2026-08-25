import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// ACRA - Load credentials from secret.properties
val secretProperties = Properties().apply {
    val file = rootProject.file("secret.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.parcelize)
}

configure<ApplicationExtension> {
    namespace = "org.cssnr.noaaweather"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.cssnr.noaaweather"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ACRA - Acrarium backend setup: https://www.acra.ch/docs/Setup
        buildConfigField("String", "ACRA_URI", "\"${secretProperties.getProperty("acra.uri") ?: ""}\"")
        buildConfigField("String", "ACRA_USER", "\"${secretProperties.getProperty("acra.user") ?: ""}\"")
        buildConfigField("String", "ACRA_PASS", "\"${secretProperties.getProperty("acra.pass") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    //kotlinOptions {
    //    jvmTarget = "17"
    //}
    //tasks.withType<KotlinJvmCompile>().configureEach {
    //    compilerOptions {
    //        jvmTarget.set(JvmTarget.JVM_17)
    //    }
    //}
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    // TODO: Verify this is correct...
    configurations.all {
        exclude(group = "com.intellij", module = "annotations")
    }
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.play.services.location)
    implementation(libs.glide)
    implementation(libs.okhttp3.integration)
    implementation(libs.acra.http)
    implementation(libs.acra.toast)
    //implementation(libs.timber)
    ksp(libs.glide.ksp)
    ksp(libs.androidx.room.compiler)
    ksp(libs.moshi.kotlin.codegen)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
