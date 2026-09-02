plugins {
    alias(libs.plugins.android.application)
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
    enableKotlin = false
    namespace = "com.tideglass.surf.watchface"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tideglass.surf.watchface"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
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

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = signingConfigs.getByName(if (uploadSigningConfigured) "upload" else "debug")
        }
    }
}
