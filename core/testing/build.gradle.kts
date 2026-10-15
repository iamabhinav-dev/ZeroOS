plugins {
    alias(libs.plugins.zeroos.android.library)
}

android {
    namespace = "com.zeroos.core.testing"
}

dependencies {
    implementation(project(":core:model"))
}
