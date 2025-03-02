plugins {
    alias(libs.plugins.android.application)
    id("com.chaquo.python")
}

android {
    namespace = "com.example.tnote"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tnote"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        ndk {
            abiFilters += setOf("arm64-v8a","x86_64")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        chaquopy {
            defaultConfig {
                pip {
                    // A requirement specifier, with or without a version number:
                    install("scipy")
                    install("numpy")
                    install("torch")
                    install("torchvision")
                    install("matplotlib")
//                    // An sdist or wheel filename, relative to the project directory:
//                    install("MyPackage-1.2.3-py2.py3-none-any.whl")
//
//                    // A directory containing a setup.py, relative to the project
//                    // directory (must contain at least one slash):
//                    install("./MyPackage")
//
//                    // "-r"` followed by a requirements filename, relative to the
//                    // project directory:
//                    install("-r", "requirements.txt")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}