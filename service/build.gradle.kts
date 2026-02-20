plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

val libVersion = "0.1"

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.solo4.accessibilitychecker"
            artifactId = "service"
            version = libVersion
            afterEvaluate {
                from(components["release"])
            }
        }
    }
    repositories {
        mavenLocal()
    }
}

android {
    group = "com.solo4.accessibilitychecker"
    version = libVersion

    publishing {
        singleVariant("release")
    }

    namespace = "com.solo4.accessibilitychecker.service"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    kotlinOptions {
        jvmTarget = "11"
    }

    dependencies {
        implementation(libs.androidx.appcompat)
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.core)
    }
}