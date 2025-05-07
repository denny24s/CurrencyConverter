plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id("org.jetbrains.kotlin.kapt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("com.google.dagger:hilt-core:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")
}