plugins {
    alias(libs.plugins.zeroos.android.library)
    alias(libs.plugins.zeroos.android.compose)
}

android {
    namespace = "com.zeroos.feature.applist"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:resources"))
    implementation(project(":core:logging"))
    implementation(project(":core:data"))
}
