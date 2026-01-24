import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "cn.ratnoumi.bcardtools"
    compileSdk = 36

    defaultConfig {
        applicationId = "cn.ratnoumi.bcardtools"
        minSdk = 28
        targetSdk = 36
        // 自动计算 versionCode (基于 git commit 数量)
        versionCode = getGitCommitCount()
        // 优先使用环境变量中的 VERSION_NAME (CI/CD 传入)，否则使用默认值
        versionName = System.getenv("VERSION_NAME") ?: "1.3.4"

        // 动态设置应用名称：正式版显示 "BCardTools"，非正式版显示 "BCardTools (Beta)"
        val isReleaseEnv = System.getenv("IS_RELEASE") == "true"
        val appName = if (isReleaseEnv) "BCardTools" else "BCardTools (Beta)"
        resValue("string", "app_name", appName)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Determine if we are running on GitHub Actions or locally
            val isCI = System.getenv("CI") == "true"
            
            if (isCI) {
                // In CI, we use environment variables
                storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release.jks")
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            } else {
                // Local development fallback (optional, can be configured via local.properties if needed)
                // For now, we just leave it empty or you can configure it manually
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".beta"
            // app_name 由 defaultConfig 统一管理
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.10.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    // https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on
    implementation("org.bouncycastle:bcprov-jdk18on:1.82")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

fun getGitCommitCount(): Int {
    return try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().readText().trim().toInt()
    } catch (e: Exception) {
        println("Git commit count failed: ${e.message}")
        10
    }
}