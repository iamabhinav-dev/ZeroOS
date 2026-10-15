plugins {
    alias(libs.plugins.zeroos.android.library)
    alias(libs.plugins.zeroos.android.compose)
}

android {
    namespace = "com.zeroos.core.ui"
}

dependencies {
    implementation(project(":core:resources"))
}
