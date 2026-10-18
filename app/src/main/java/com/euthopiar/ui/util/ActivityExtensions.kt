package com.euthopiar.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Traverses the [ContextWrapper] chain to resolve the hosting [Activity].
 * Crucial in Jetpack Compose where LocalContext.current is frequently wrapped
 * in ContextThemeWrapper, NavHost wrapper, or Hilt wrapper.
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
