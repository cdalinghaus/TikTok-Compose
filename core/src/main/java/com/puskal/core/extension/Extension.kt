package com.puskal.core.extension

import android.content.Context
import android.provider.Settings
import java.text.DecimalFormat

/**
 * Created by Puskal Khadka on 3/18/2023.
 */

val decimalFormat = DecimalFormat("#.#")
fun Long.formattedCount(): String {
    return this.toString()
    return when {
        this >= 1_000_000 -> String.format("%.1fM", this / 1_000_000.0)
        this >= 1_000 -> String.format("%.1fK", this / 1_000.0)
        else -> this.toString()
    }
}

fun randomUploadDate(): String = "${(1..24).random()}h"


fun Pair<String, String>.getFormattedInternationalNumber() = "${this.first}-${this.second}".trim()

fun Context.getCurrentBrightness():Float= Settings.System.getInt(this.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    .toFloat().div(255)