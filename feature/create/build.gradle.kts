plugins {
    id("plugin.android-common")
    kotlin("plugin.serialization") version "1.5.0"
}


dependencies {
    COMMON_THEME
    COMMON_COMPOSABLE
    DOMAIN
    DATA
    CORE
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2") // Use the latest version

}