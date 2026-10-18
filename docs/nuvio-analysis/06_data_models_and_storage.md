# Nuvio Architecture Reference: Data Models & Storage Schema

This document provides a comprehensive, source-grounded specification of all data schemas, JSON payloads, local persistent stores, Supabase relational models, and core Kotlin multiplatform entity models in Nuvio.

---

## 1. Stremio Addon Protocol Schemas

### 1.1 Manifest Schema (`AddonManifest`)
Defined in [AddonModels.kt:8-20](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/addons/AddonModels.kt#L8-L20). Returned by `GET {transportUrl}/manifest.json`.

```json
{
  "id": "org.stremio.cinemeta",
  "name": "Cinemeta",
  "description": "The official movie and TV show catalog addon for Stremio",
  "version": "3.0.12",
  "logo": "https://images.stremio.com/cinemeta-logo.png",
  "resources": [
    "catalog",
    {
      "name": "meta",
      "types": ["movie", "series"],
      "idPrefixes": ["tt", "tmdb:"]
    },
    {
      "name": "stream",
      "types": ["movie", "series"],
      "idPrefixes": ["tt"]
    }
  ],
  "types": ["movie", "series", "anime", "other"],
  "idPrefixes": ["tt", "tmdb:"],
  "catalogs": [
    {
      "type": "movie",
      "id": "top",
      "name": "Popular Movies",
      "extra": [
        {
          "name": "genre",
          "isRequired": false,
          "options": ["Action", "Comedy", "Drama", "Sci-Fi"]
        },
        {
          "name": "skip",
          "isRequired": false
        },
        {
          "name": "search",
          "isRequired": false
        }
      ]
    }
  ],
  "behaviorHints": {
    "configurable": false,
    "configurationRequired": false,
    "adult": false,
    "p2p": false
  }
}
```

### 1.2 Stream Item Schema (`StreamItem`, `StreamBehaviorHints`)
Defined in [StreamModels.kt:15-36](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamModels.kt#L15-L36) and [StreamModels.kt:205-217](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamModels.kt#L205-L217). Returned by `GET {transportUrl}/stream/{type}/{id}.json`.

```json
{
  "name": "Torrentio\n1080p",
  "title": "Big Buck Bunny 1080p BluRay x264\n💾 1.45 GB 👥 124 ⚙️ Debrid",
  "description": "Stream details description text",
  "url": "https://debrid.provider.download/link/...",
  "infoHash": "4161726a11e0f09ca8892415175955681177651a",
  "fileIdx": 0,
  "sources": [
    "tracker:udp://tracker.opentrackr.org:1337/announce",
    "dht:4161726a11e0f09ca8892415175955681177651a"
  ],
  "behaviorHints": {
    "bingeGroup": "torrentio|1080p|h264",
    "notWebReady": false,
    "videoHash": "e2a4b8...",
    "videoSize": 1558231040,
    "filename": "Big.Buck.Bunny.1080p.mkv",
    "proxyHeaders": {
      "request": {
        "User-Agent": "NuvioPlaybackEngine/1.0",
        "Referer": "https://streaming-cdn.com/",
        "Cookie": "session_id=abc123xyz"
      },
      "response": {
        "Content-Type": "application/vnd.apple.mpegurl"
      }
    }
  },
  "subtitles": [
    {
      "id": "sub_en_0",
      "url": "https://subtitles.org/sub_en.srt",
      "lang": "eng"
    }
  ]
}
```

---

## 2. QuickJS Scraper Plugin Protocol Schema

Third-party scrapers executed inside the QuickJS runtime conform to the following manifest and execution payload contract:

### 2.1 Plugin Manifest (`plugin.json`)
```json
{
  "id": "nuvio.plugin.provider_example",
  "name": "Example Streams Provider",
  "version": "1.2.0",
  "author": "Community",
  "entrypoint": "index.js",
  "capabilities": ["streams", "subtitles"],
  "permissions": ["network", "crypto"],
  "supportedTypes": ["movie", "series"]
}
```

### 2.2 Scraper Method Contract
Every plugin JavaScript bundle must export a global or default object implementing:
```javascript
async function resolveStreams(request) {
  // request = {
  //   type: "movie" | "series",
  //   imdbId: "tt0111161",
  //   tmdbId: 278,
  //   title: "The Shawshank Redemption",
  //   year: 1994,
  //   season: 1,      // null for movies
  //   episode: 1      // null for movies
  // }
  return [
    {
      name: "Provider Fast CDN",
      title: "1080p 60fps H.264",
      url: "https://video.example.com/playlist.m3u8",
      headers: {
        "User-Agent": "Mozilla/5.0 ...",
        "Referer": "https://embed.example.com/"
      },
      quality: "1080p"
    }
  ];
}
```

---

## 3. Supabase Relational Database Schema

When cloud account sync is enabled, Nuvio connects to Supabase PostgREST tables:

### 3.1 `profiles`
```sql
CREATE TABLE profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    profile_index INT NOT NULL,
    name TEXT NOT NULL,
    avatar_id TEXT,
    avatar_url TEXT,
    profile_background_id TEXT,
    uses_primary_addons BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 3.2 `library_items`
```sql
CREATE TABLE library_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    content_id TEXT NOT NULL,
    type TEXT NOT NULL,
    name TEXT NOT NULL,
    poster TEXT,
    banner TEXT,
    logo TEXT,
    description TEXT,
    release_info TEXT,
    imdb_rating TEXT,
    genres TEXT[],
    list_keys TEXT[],
    saved_at_epoch_ms BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(profile_id, content_id)
);
```

### 3.3 `watched_items`
```sql
CREATE TABLE watched_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    item_key TEXT NOT NULL,
    is_watched BOOLEAN NOT NULL DEFAULT TRUE,
    watched_at_epoch_ms BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(profile_id, item_key)
);
```

### 3.4 `watch_progress`
```sql
CREATE TABLE watch_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    item_key TEXT NOT NULL,
    position_ms BIGINT NOT NULL,
    duration_ms BIGINT NOT NULL,
    season_number INT,
    episode_number INT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(profile_id, item_key)
);
```

### 3.5 `collections`
```sql
CREATE TABLE collections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    cover_image_url TEXT,
    item_ids TEXT[],
    created_at_epoch_ms BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## 4. Local File Storage Layout (`context.filesDir`)

| File Name | Serializer | Root Class | Description |
|---|---|---|---|
| `library_store.json` | `kotlinx.serialization` | `List<LibraryItem>` | Complete local library watchlist with user tags and categories |
| `watched_store.json` | `kotlinx.serialization` | `StoredWatchedPayload` | Map of `itemKey -> WatchedEntry` tracking fully watched status |
| `watch_progress_store.json`| `kotlinx.serialization`| `WatchProgressStorage` | Map of `logicalKey -> ProgressRecord` (position, duration, timestamp) |
| `collections_store.json` | `kotlinx.serialization` | `List<CollectionFolder>` | User-curated custom folders and collection categories |
| `addons_store.json` | `kotlinx.serialization` | `List<ManagedAddon>` | Installed Stremio manifests with enabled/disabled states |
| `plugins_store.json` | `kotlinx.serialization` | `List<ManagedPlugin>` | Installed QuickJS scraper scripts and manifests |
| `downloads_store.json` | `kotlinx.serialization` | `List<DownloadItem>` | Active, paused, and completed downloads with local file URIs |
| `custom_posters_store.json`| `kotlinx.serialization`| `CustomPosterStorage` | Custom poster patterns and active screen filters |

---

## 5. Domain Data Entity Models

### 5.1 Meta Details & Episodes (`MetaDetailsModels.kt`)
Defined in [MetaDetailsModels.kt:15-80](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/MetaDetailsModels.kt#L15-L80):

```kotlin
data class MetaDetails(
    val id: String,
    val type: String,
    val name: String,
    val genres: List<String> = emptyList(),
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val runtime: String? = null,
    val imdbRating: String? = null,
    val cast: List<String> = emptyList(),
    val directors: List<String> = emptyList(),
    val writers: List<String> = emptyList(),
    val videos: List<MetaVideo> = emptyList(),
    val similar: List<MetaPreview> = emptyList(),
    val trailerYtId: String? = null,
    val links: List<MetaLink> = emptyList()
)

data class MetaVideo(
    val id: String,
    val title: String,
    val released: String? = null,
    val thumbnail: String? = null,
    val episode: Int? = null,
    val season: Int? = null,
    val overview: String? = null,
    val streams: List<StreamItem> = emptyList()
)
```

### 5.2 Download Entity (`DownloadsModels.kt`)
Defined in [DownloadsModels.kt:23-54](file:///C:/Kently/Nuvio%20Apk/NuvioMobile/composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsModels.kt#L23-L54):

```kotlin
@Serializable
data class DownloadItem(
    val id: String,
    val contentType: String,
    val parentMetaId: String,
    val parentMetaType: String,
    val videoId: String,
    val title: String,
    val logo: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val episodeThumbnail: String? = null,
    val streamTitle: String,
    val streamSubtitle: String? = null,
    val providerName: String,
    val providerAddonId: String? = null,
    val sourceUrl: String,
    val sourceHeaders: Map<String, String> = emptyMap(),
    val sourceResponseHeaders: Map<String, String> = emptyMap(),
    val subtitleRequests: List<SubtitleAddonRequest> = emptyList(),
    val sourceSubtitles: List<StreamSubtitle> = emptyList(),
    val localFileUri: String? = null,
    val fileName: String,
    val status: DownloadStatus,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val errorMessage: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
```
