plugins {
    alias(libs.plugins.zeroos.android.library)
}

android {
    namespace = "com.zeroos.core.data"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:logging"))
}
