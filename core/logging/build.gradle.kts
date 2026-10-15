plugins {
    alias(libs.plugins.zeroos.android.library)
}

android {
    namespace = "com.zeroos.core.logging"
}

dependencies {
    implementation(libs.androidx.annotation)
    testImplementation(project(":core:testing"))
}
