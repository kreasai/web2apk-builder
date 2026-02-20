plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun prop(name: String, fallback: String): String {
    return if (project.hasProperty(name)) {
        (project.property(name) as String).ifBlank { fallback }
    } else {
        fallback
    }
}

fun boolProp(name: String, fallback: Boolean): Boolean {
    return prop(name, if (fallback) "true" else "false").toBooleanStrictOrNull() ?: fallback
}

val appName = prop("web2apkAppName", "Web2APK")
val applicationIdValue = prop("web2apkApplicationId", "com.kreasai.web2apk")
val versionNameValue = prop("web2apkVersionName", "1.0.0")
val versionCodeValue = prop("web2apkVersionCode", "1").toIntOrNull() ?: 1
val webUrl = prop("web2apkWebUrl", "https://example.com")
val permissionsCsv = prop("web2apkPermissionsCsv", "")
val enableSwipeRefresh = boolProp("web2apkEnableSwipeRefresh", true)
val enableExternalApps = boolProp("web2apkEnableExternalApps", true)
val enableOfflinePage = boolProp("web2apkEnableOfflinePage", true)
val enableBackNavigation = boolProp("web2apkEnableBackNavigation", true)
val enableSplash = boolProp("web2apkEnableSplash", false)
val splashBgColor = prop("web2apkSplashBackgroundColor", "#111827")
val splashLogoUrl = prop("web2apkSplashLogoUrl", "")

android {
    namespace = "com.kreasai.web2apk.template"
    compileSdk = 34

    defaultConfig {
        applicationId = applicationIdValue
        minSdk = 24
        targetSdk = 34
        versionCode = versionCodeValue
        versionName = versionNameValue

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "WEB_URL", "\"${webUrl.replace("\"", "\\\"")}\"")
        buildConfigField("String", "PERMISSIONS_CSV", "\"${permissionsCsv.replace("\"", "\\\"")}\"")
        buildConfigField("boolean", "ENABLE_SWIPE_REFRESH", enableSwipeRefresh.toString())
        buildConfigField("boolean", "ENABLE_EXTERNAL_APPS", enableExternalApps.toString())
        buildConfigField("boolean", "ENABLE_OFFLINE_PAGE", enableOfflinePage.toString())
        buildConfigField("boolean", "ENABLE_BACK_NAVIGATION", enableBackNavigation.toString())
        buildConfigField("boolean", "ENABLE_SPLASH", enableSplash.toString())
        buildConfigField("String", "SPLASH_BACKGROUND_COLOR", "\"${splashBgColor.replace("\"", "\\\"")}\"")
        buildConfigField("String", "SPLASH_LOGO_URL", "\"${splashLogoUrl.replace("\"", "\\\"")}\"")
        resValue("string", "app_name", appName.replace("\"", "\\\""))
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.webkit:webkit:1.11.0")
}
