plugins {
    alias(libs.plugins.zeroos.android.library)
    alias(libs.plugins.zeroos.android.compose)
}

android {
    namespace = "com.zeroos.core.ui"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:resources"))
    // Back handling and resume checks for the shared permission explanation (Step 4.1).
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
