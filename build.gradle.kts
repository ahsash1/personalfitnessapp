// Top-level build file where you can add configuration options common to all sub-projects/modules.

// AGP 9's built-in Kotlin support has a runtime dependency on a specific Kotlin
// Gradle Plugin (KGP) version, which can lag behind libraries compiled against a
// newer Kotlin stdlib. Overriding the classpath bumps the compiler AGP uses
// internally without opting out of built-in Kotlin (which would require
// android.builtInKotlin=false and reintroduces the plugin-application path AGP 9
// deliberately blocks). See:
// https://developer.android.com/build/releases/agp-9-0-0-release-notes#android-gradle-plugin-built-in-kotlin
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.12")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}