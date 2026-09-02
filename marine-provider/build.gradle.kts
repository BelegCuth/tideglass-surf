plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val uploadStorePath = providers.environmentVariable("TIDEGLASS_UPLOAD_STORE_FILE").orNull
val uploadStorePassword = providers.environmentVariable("TIDEGLASS_UPLOAD_STORE_PASSWORD").orNull
val uploadKeyAlias = providers.environmentVariable("TIDEGLASS_UPLOAD_KEY_ALIAS").orNull
val uploadKeyPassword = providers.environmentVariable("TIDEGLASS_UPLOAD_KEY_PASSWORD").orNull
val uploadSigningConfigured = listOf(uploadStorePath, uploadStorePassword, uploadKeyAlias, uploadKeyPassword)
    .all { !it.isNullOrBlank() }
val requireReleaseSigning = providers.gradleProperty("tideglass.requireReleaseSigning")
    .orNull?.toBooleanStrictOrNull() ?: false

if (requireReleaseSigning && !uploadSigningConfigured) {
    error("A complete Tideglass upload signing configuration is required")
}

android {
    namespace = "com.tideglass.surf.provider"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tideglass.surf.provider"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        val dataBaseUrl = providers.gradleProperty("tideglass.dataBaseUrl").orElse("").get()
        buildConfigField("String", "DATA_BASE_URL", "\"${dataBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (uploadSigningConfigured) {
            create("upload") {
                storeFile = file(uploadStorePath!!)
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName(if (uploadSigningConfigured) "upload" else "debug")
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        disable += "AndroidGradlePluginVersion"
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.watchface.complications.data.source.ktx)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.json)
}
