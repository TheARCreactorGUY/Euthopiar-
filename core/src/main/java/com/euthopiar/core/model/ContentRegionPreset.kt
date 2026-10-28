package com.euthopiar.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ContentRegionPreset(
    val id: String,
    val countryGroup: String,
    val displayName: String,
    val subtitle: String,
    val regionCode: String?,
    val originCountry: String?,
    val originalLanguages: String?,
    val cleanName: String = displayName.replace(Regex("\\s*\\([^)]*\\)"), "").trim()
) {
    GLOBAL("global", "Global", "Global (Default)", "Worldwide trending movies & series", null, null, null, "Global"),
    INDIA_ALL("india_all", "India", "Pan-India (All)", "Hindi, Telugu, Tamil, Malayalam, Kannada & regional hits", "IN", "IN", "hi|te|ta|ml|kn|bn|mr|pa", "Pan-India"),
    SOUTH_INDIA_ALL("south_india_all", "India (South)", "South India (All)", "Telugu, Tamil, Malayalam & Kannada cinema & series", "IN", "IN", "te|ta|ml|kn", "South India"),
    SOUTH_INDIA_TELUGU("south_india_te", "India (South)", "Telugu (Tollywood)", "Telugu blockbusters, theatrical releases & web series", "IN", "IN", "te", "Telugu"),
    SOUTH_INDIA_TAMIL("south_india_ta", "India (South)", "Tamil (Kollywood)", "Tamil cinema, theatrical releases & web series", "IN", "IN", "ta", "Tamil"),
    SOUTH_INDIA_MALAYALAM("south_india_ml", "India (South)", "Malayalam (Mollywood)", "Critically acclaimed Malayalam cinema & series", "IN", "IN", "ml", "Malayalam"),
    SOUTH_INDIA_KANNADA("south_india_kn", "India (South)", "Kannada (Sandalwood)", "Kannada blockbusters, theatrical releases & series", "IN", "IN", "kn", "Kannada"),
    NORTH_INDIA_HINDI("north_india_hi", "India (North)", "North India (Hindi / Punjabi)", "Bollywood, Hindi & Punjabi movies and series", "IN", "IN", "hi|pa", "North India"),
    UNITED_STATES("us", "International", "United States", "Hollywood & US network releases", "US", "US", "en", "United States"),
    UNITED_KINGDOM("gb", "International", "United Kingdom", "British cinema & television", "GB", "GB", "en", "United Kingdom"),
    SOUTH_KOREA("kr", "International", "South Korea", "K-Dramas & Korean cinema", "KR", "KR", "ko", "South Korea"),
    JAPAN("jp", "International", "Japan", "Japanese cinema, anime & live-action", "JP", "JP", "ja", "Japan");

    val isIndianRegion: Boolean get() = regionCode == "IN" || countryGroup.startsWith("India")

    companion object {
        fun fromId(id: String?): ContentRegionPreset = entries.firstOrNull { it.id == id } ?: GLOBAL
    }
}
