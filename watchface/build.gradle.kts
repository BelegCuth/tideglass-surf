plugins {
    alias(libs.plugins.android.application)
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

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            // Replace with a private upload key before Play Console release.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
