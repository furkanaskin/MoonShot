package com.haroldadmin.moonshot.utils

import android.content.res.Configuration
import androidx.core.os.ConfigurationCompat
import java.text.SimpleDateFormat
import java.util.*

fun Date.format(configuration: Configuration, pattern: String = "dd-MM-YYYY"): String {
    val locale = ConfigurationCompat.getLocales(configuration).get(0)
    val formatter = SimpleDateFormat(pattern, locale)
    return formatter.format(this)
}