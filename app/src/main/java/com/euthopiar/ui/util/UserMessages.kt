package com.euthopiar.ui.util

import android.content.Context
import androidx.annotation.StringRes
import com.euthopiar.R
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Standardized user-facing error model.
 * Translates low-level network, extractor, and repository exceptions
 * into calm, human-friendly guidance without technical jargon or stack traces.
 */
sealed interface UserError {
    object Offline : UserError
    object Timeout : UserError
    object SourceUnavailable : UserError
    object NoSources : UserError
    data class NoResults(val query: String = "") : UserError
    object Unknown : UserError
}

/**
 * Maps a technical [Throwable] to a calm, consumer-facing [UserError].
 */
fun Throwable.toUserError(): UserError {
    return when (this) {
        is UnknownHostException, is ConnectException -> UserError.Offline
        is SocketTimeoutException -> UserError.Timeout
        is IOException -> {
            val msg = message?.lowercase().orEmpty()
            if (msg.contains("network") || msg.contains("connection") || msg.contains("offline") || msg.contains("unreachable")) {
                UserError.Offline
            } else if (msg.contains("timeout") || msg.contains("timed out")) {
                UserError.Timeout
            } else {
                UserError.SourceUnavailable
            }
        }
        else -> {
            val msg = message?.lowercase().orEmpty()
            if (msg.contains("no source") || msg.contains("no plugin") || msg.contains("no playable")) {
                UserError.NoSources
            } else if (msg.contains("timeout") || msg.contains("timed out")) {
                UserError.Timeout
            } else if (msg.contains("offline") || msg.contains("unreachable") || msg.contains("connection")) {
                UserError.Offline
            } else {
                UserError.Unknown
            }
        }
    }
}

/**
 * Returns the string resource ID for the title of this [UserError].
 */
@StringRes
fun UserError.title(): Int = when (this) {
    is UserError.Offline -> R.string.error_offline_title
    is UserError.Timeout -> R.string.error_generic_title
    is UserError.SourceUnavailable -> R.string.error_source_failed_title
    is UserError.NoSources -> R.string.error_no_sources_title
    is UserError.NoResults -> if (query.isNotBlank()) R.string.error_no_results_query_title else R.string.error_no_results_title
    is UserError.Unknown -> R.string.error_generic_title
}

/**
 * Returns the string resource ID for the hint/message of this [UserError].
 */
@StringRes
fun UserError.hint(): Int = when (this) {
    is UserError.Offline -> R.string.error_offline_hint
    is UserError.Timeout -> R.string.error_generic_hint
    is UserError.SourceUnavailable -> R.string.error_source_failed_hint
    is UserError.NoSources -> R.string.error_no_sources_hint
    is UserError.NoResults -> R.string.error_no_results_hint
    is UserError.Unknown -> R.string.error_generic_hint
}

/**
 * Returns the localized title string, safely formatted if parametrized (e.g. search query).
 */
fun UserError.getTitle(context: Context): String = when (this) {
    is UserError.NoResults -> {
        if (query.isNotBlank()) {
            context.getString(R.string.error_no_results_query_title, query)
        } else {
            context.getString(R.string.error_no_results_title)
        }
    }
    else -> context.getString(title())
}

/**
 * Returns the localized hint string.
 */
fun UserError.getHint(context: Context): String = context.getString(hint())
