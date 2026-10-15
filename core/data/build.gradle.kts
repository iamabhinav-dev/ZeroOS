plugins {
    alias(libs.plugins.zeroos.android.library)
}

android {
    namespace = "com.zeroos.core.data"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:logging"))
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(project(":core:testing"))
}
