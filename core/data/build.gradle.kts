plugins {
    alias(libs.plugins.zeroos.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.zeroos.core.data"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:logging"))
    implementation(libs.kotlinx.coroutines.android)
    api(libs.androidx.datastore)

    testImplementation(project(":core:testing"))
}
