package com.euthopiar.core.network

import okhttp3.Headers

/**
 * Detects Cloudflare Turnstile, Bot Fight Mode, and anti-bot mitigation challenges.
 */
object ChallengeDetector {

    fun isChallenge(code: Int, headers: Headers, bodyHead: String): Boolean {
        if (code != 403 && code != 503) return false

        val cfMitigated = headers["cf-mitigated"]
        if (cfMitigated.equals("challenge", ignoreCase = true)) return true

        return "<title>Just a moment" in bodyHead ||
                "<title>Attention Required" in bodyHead ||
                "challenges.cloudflare.com/turnstile" in bodyHead ||
                "cf-browser-verification" in bodyHead
    }
}
