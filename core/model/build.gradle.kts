plugins {
    alias(libs.plugins.zeroos.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.zeroos.core.model"
}

dependencies {
    api(libs.kotlinx.serialization.json)
}
