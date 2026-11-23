plugins {
    id("com.android.application")
}
android {
    namespace = "com.abolfazl.telexvip"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.abolfazl.telexvip"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release { isMinifyEnabled = false }
        debug { isMinifyEnabled = false }
    }
}
dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")
}
