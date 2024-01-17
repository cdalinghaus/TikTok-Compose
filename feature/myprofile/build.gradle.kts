plugins {
    id("plugin.android-common")
}


dependencies {
    implementation(project(mapOf("path" to ":feature:loginwithemailphone")))
    COMMON_THEME
    COMMON_COMPOSABLE
    DOMAIN
    DATA
    CORE
}