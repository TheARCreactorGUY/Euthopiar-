# Nuvio Architecture Reference - Coverage Ledger

This ledger inventories **every source file**, configuration, resource, native library, and specification in the Nuvio application codebase (`NuvioMobile`).

- **Total Source Files Tracked**: 1,152
- **Documented Files**: 1,152 (100% complete)
- **Skipped Non-Source Files**: 402 (tests, drawables, fonts, Xcode project metadata)

Status values: `TODO` (pending reading), `READ` (fully read and analyzed), `DOCUMENTED` (synthesized into reference documents).


### Build & Configuration

| File Path | Type / Role | Lines | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `androidApp/build.gradle.kts` | Build Script | 186 | DOCUMENTED |  |
| `build.gradle.kts` | Build Script | 11 | DOCUMENTED |  |
| `composeApp/build.gradle.kts` | Build Script | 558 | DOCUMENTED |  |
| `gradle.properties` | Source Component | 14 | DOCUMENTED |  |
| `gradle/libs.versions.toml` | Build Script | 106 | DOCUMENTED |  |
| `iosApp/Configuration/Config.xcconfig` | Configuration | 6 | DOCUMENTED |  |
| `iosApp/Configuration/Version.xcconfig` | Configuration | 2 | DOCUMENTED |  |
| `settings.gradle.kts` | Build Script | 32 | DOCUMENTED |  |

### Android App Shell

| File Path | Type / Role | Lines | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `androidApp/src/main/kotlin/com/nuvio/app/launcher/AppIconActivities.kt` | Source Component | 15 | DOCUMENTED |  |

### Core: Core

| File Path | Type / Role | Lines | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/auth/AuthModels.kt` | Data Model | 20 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/auth/AuthRepository.kt` | Repository | 287 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/auth/AuthStorage.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/auth/DeviceLinkAuthRepository.kt` | Repository | 229 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/auth/DeviceSessionRegistration.kt` | Source Component | 90 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/concurrent/ConflatedTaskDispatcher.kt` | Source Component | 60 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/deeplink/AppUrlBridge.kt` | Source Component | 178 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/format/LocalDateTimeDisplay.kt` | Source Component | 3 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/format/ReleaseDateDisplay.kt` | Source Component | 33 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/i18n/LocalizedUiText.kt` | Source Component | 178 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/i18n/MediaStatusLabel.kt` | Source Component | 22 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/network/ApiHttpClient.kt` | Source Component | 36 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/network/BackendRateLimit.kt` | Source Component | 136 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/network/NetworkStatusRepository.kt` | Repository | 175 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/network/ServerConfiguration.kt` | Configuration | 74 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/network/ServerConfigurationStorage.kt` | Configuration | 7 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/network/ServerDiscovery.kt` | Source Component | 189 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/network/SupabaseEndpointConfig.kt` | Configuration | 66 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/network/SupabaseProvider.kt` | Source Component | 119 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/poster/CustomPosterFallbackInterceptor.kt` | Source Component | 36 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/poster/CustomPosterOverlay.kt` | Source Component | 199 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/poster/CustomPosterScreen.kt` | Screen UI (Compose) | 31 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/poster/CustomPosterUrlRepository.kt` | Repository | 73 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/poster/CustomPosterUrlResolver.kt` | Stream / Content Resolver | 264 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/poster/CustomPosterUrlStorage.kt` | Source Component | 8 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/storage/LocalAccountDataCleaner.kt` | Source Component | 103 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/storage/ProfileScopedKey.kt` | Source Component | 9 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/sync/AppForegroundMonitor.kt` | Source Component | 12 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/sync/MediaServerSync.kt` | Source Component | 128 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/sync/ProfileSettingsCredentialPolicy.kt` | Source Component | 48 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/sync/ProfileSettingsSync.kt` | Source Component | 418 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/sync/ProviderCredentialModels.kt` | Data Model | 61 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/sync/ProviderCredentialSync.kt` | Source Component | 303 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/sync/SyncClientIdentity.kt` | Source Component | 50 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/sync/SyncManager.kt` | Source Component | 570 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/sync/SyncPlatform.kt` | Source Component | 4 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/sync/SyncPreferenceJson.kt` | Source Component | 89 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/time/EpisodeReleaseDateParser.kt` | Parser | 171 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/time/EpisodeReleaseDatePlatform.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/tracking/TrackingProviderBootstrap.kt` | Source Component | 34 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/AppIconPainter.kt` | Source Component | 18 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/AppTheme.kt` | Source Component | 50 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/AshSwarmRenderer.kt` | Source Component | 17 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/BottomSheet.kt` | Bottom Sheet (Compose) | 174 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/CardDepthEffect.kt` | Source Component | 109 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/CardDepthStyleRepository.kt` | Repository | 175 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/CardDepthStyleStorage.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/Chip.kt` | Source Component | 55 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/Components.kt` | Source Component | 548 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/ContinueWatchingActionSheet.kt` | Bottom Sheet (Compose) | 175 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/ContinueWatchingText.kt` | Source Component | 25 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/CustomThemeColors.kt` | Source Component | 43 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/CustomThemePalette.kt` | Source Component | 28 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/Dialog.kt` | Dialog UI (Compose) | 226 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/DisintegrationEffect.kt` | Source Component | 263 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/DisintegrationRequest.kt` | Source Component | 33 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/DropdownChip.kt` | Source Component | 165 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/DuplicateSafeLazyKeys.kt` | Source Component | 23 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/FloatingNavigationBar.kt` | Source Component | 38 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/FloatingPrompt.kt` | Source Component | 271 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/Gradients.kt` | Source Component | 24 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/HeroStretchOverscroll.kt` | Source Component | 147 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/HorizontalScrollModifiers.kt` | Source Component | 46 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/HsvColor.kt` | Source Component | 29 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/LoadingIndicator.kt` | Source Component | 86 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/Menu.kt` | Source Component | 95 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/NativeBottomSheet.kt` | Bottom Sheet (Compose) | 23 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/NativeTabBridge.kt` | Source Component | 221 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/NavigationBar.kt` | Source Component | 528 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/NetworkOfflineCard.kt` | Source Component | 43 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/PlatformBackHandler.kt` | Source Component | 9 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/PlatformExitApp.kt` | Source Component | 3 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/PlatformImageLoader.kt` | Source Component | 13 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/PlatformInsets.kt` | Source Component | 36 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/PosterActionSheet.kt` | Bottom Sheet (Compose) | 73 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/PosterCardDimensions.kt` | Source Component | 13 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/PosterCardStyleCompose.kt` | Source Component | 12 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/PosterCardStyleRepository.kt` | Repository | 150 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/PosterCardStyleStorage.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/PosterLift.kt` | Source Component | 133 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/PosterOpenMotion.kt` | Source Component | 50 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/PosterZoomActionOverlay.kt` | Source Component | 555 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/ProfileMeshBackground.kt` | Source Component | 91 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/ProgressBar.kt` | Source Component | 41 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/ScopedDisintegrationTracker.kt` | Source Component | 90 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/ScreenActivity.kt` | Screen UI (Compose) | 17 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/ShelfComponents.kt` | Source Component | 381 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/Shimmer.kt` | Source Component | 146 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/Skeleton.kt` | Source Component | 92 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/Theme.kt` | Source Component | 228 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/ThemeAccent.kt` | Source Component | 22 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/ThemeColors.kt` | Source Component | 194 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/TokenPreview.kt` | Source Component | 85 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/Tokens.kt` | Source Component | 526 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/TrackingListPickerDialog.kt` | Dialog UI (Compose) | 117 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/TypeScale.kt` | Source Component | 39 | DOCUMENTED |  |

### Navigation Architecture

| File Path | Type / Role | Lines | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/navigation/NativeNavigation.kt` | Source Component | 9 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/navigation/NuvioNavigator.kt` | Source Component | 81 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/navigation/PosterNavigationArtwork.kt` | Source Component | 50 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/navigation/PosterNavigationMotion.kt` | Source Component | 63 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/navigation/PosterNavigationState.kt` | Source Component | 85 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/navigation/PosterNavigationTransition.kt` | Source Component | 108 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/navigation/RouteDisposalNavEntryDecorator.kt` | Source Component | 35 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/navigation/Routes.kt` | Source Component | 152 | DOCUMENTED |  |

### Feature: Features

| File Path | Type / Role | Lines | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/addons/AddonHttpClient.kt` | Source Component | 14 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/addons/AddonManifestParser.kt` | Parser | 141 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/addons/AddonModels.kt` | Data Model | 114 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/addons/AddonPlatform.kt` | Source Component | 47 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/addons/AddonRepository.kt` | Repository | 491 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/addons/AddonTransportUrls.kt` | Source Component | 50 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/addons/AddonsScreen.kt` | Screen UI (Compose) | 640 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/auth/AuthScreen.kt` | Screen UI (Compose) | 1190 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/auth/DeviceLinkAuthSection.kt` | Source Component | 197 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/auth/ServerConnectionController.kt` | Source Component | 128 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/auth/ServerConnectionDialogs.kt` | Dialog UI (Compose) | 438 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/catalog/CatalogData.kt` | Source Component | 224 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/catalog/CatalogModels.kt` | Data Model | 19 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/catalog/CatalogRepository.kt` | Repository | 234 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/catalog/CatalogScreen.kt` | Screen UI (Compose) | 430 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/catalog/CatalogTarget.kt` | Source Component | 49 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/catalog/LibraryCatalogState.kt` | Source Component | 53 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/cloud/CloudLibraryModels.kt` | Data Model | 122 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/cloud/CloudLibraryProviderApi.kt` | Source Component | 30 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/cloud/CloudLibraryRepository.kt` | Repository | 315 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/cloud/PremiumizeCloudLibraryProviderApi.kt` | Source Component | 164 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/cloud/TorboxCloudLibraryProviderApi.kt` | Source Component | 242 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/CollectionCatalogResolver.kt` | Stream / Content Resolver | 44 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/CollectionEditorRepository.kt` | Repository | 944 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/CollectionEditorScreen.kt` | Screen UI (Compose) | 2610 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/CollectionJsonPreserver.kt` | Source Component | 170 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/CollectionManagementScreen.kt` | Screen UI (Compose) | 421 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/CollectionMobileSettingsRepository.kt` | Repository | 155 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/CollectionMobileSettingsStorage.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/CollectionModels.kt` | Data Model | 272 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/CollectionRepository.kt` | Repository | 343 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/CollectionStorage.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/CollectionSyncService.kt` | Android Service / API | 144 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/FolderDetailRepository.kt` | Repository | 499 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/FolderDetailScreen.kt` | Screen UI (Compose) | 440 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/collection/TmdbCollectionSourceResolver.kt` | Stream / Content Resolver | 720 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/DebridApiClients.kt` | Source Component | 597 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/DebridApiModels.kt` | Data Model | 252 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/DebridFileSelectors.kt` | Source Component | 141 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/DebridMagnetBuilder.kt` | Source Component | 37 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/DebridProvider.kt` | Source Component | 133 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/DebridProviderApis.kt` | Source Component | 424 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/DebridSettings.kt` | Source Component | 305 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/DebridSettingsRepository.kt` | Repository | 542 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/DebridSettingsStorage.kt` | Source Component | 43 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/DebridStreamFormatter.kt` | Source Component | 181 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/DebridStreamFormatterDefaults.kt` | Source Component | 11 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/DebridStreamPresentation.kt` | Source Component | 457 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/DebridStreamTemplateEngine.kt` | Source Component | 396 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/DebridUrlEncoding.kt` | Source Component | 38 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/DirectDebridResolver.kt` | Stream / Content Resolver | 375 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/DirectDebridStreamPreparer.kt` | Source Component | 205 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/LocalDebridAvailabilityService.kt` | Android Service / API | 129 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/debrid/LocalDebridService.kt` | Android Service / API | 82 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/CastSharedTransition.kt` | Source Component | 11 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/DetailsContentReveal.kt` | Source Component | 25 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/EpisodeRatingsVisibility.kt` | Source Component | 21 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/HeroTrailerAudioState.kt` | Source Component | 14 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/HeroTrailerSelector.kt` | Source Component | 37 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/ImdbEpisodeRatingsRepository.kt` | Repository | 112 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/MetaDetailsModels.kt` | Data Model | 109 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/MetaDetailsParser.kt` | Parser | 388 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/MetaDetailsReleaseLine.kt` | Source Component | 35 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/MetaDetailsRepository.kt` | Repository | 634 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/MetaDetailsScreen.kt` | Screen UI (Compose) | 2543 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/MetaScreenSettingsRepository.kt` | Screen UI (Compose) | 500 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/MetaScreenSettingsStorage.kt` | Screen UI (Compose) | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/PersonDetail.kt` | Source Component | 16 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/PersonDetailScreen.kt` | Screen UI (Compose) | 1236 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/RuntimeFormat.kt` | Runtime Engine | 56 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/SeasonViewMode.kt` | Source Component | 30 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/SeriesGraphApi.kt` | Source Component | 65 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/SeriesPlaybackResolver.kt` | Stream / Content Resolver | 285 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/SeriesSeasonSupport.kt` | Source Component | 42 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/TmdbEntityBrowseScreen.kt` | Screen UI (Compose) | 666 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/BackdropPaletteImage.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/CommentDetailSheet.kt` | Bottom Sheet (Compose) | 244 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/DetailActionButtons.kt` | Source Component | 345 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/DetailAdditionalInfoSection.kt` | Source Component | 117 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/DetailCastSection.kt` | Source Component | 253 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/DetailCommentsSection.kt` | Source Component | 295 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/DetailFloatingHeader.kt` | Source Component | 190 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/DetailHero.kt` | Source Component | 275 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/DetailMetaInfo.kt` | Source Component | 409 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/DetailPosterRailSection.kt` | Source Component | 134 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/DetailProductionSection.kt` | Source Component | 146 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/DetailSection.kt` | Source Component | 51 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/DetailSeriesContent.kt` | Source Component | 1512 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/DetailTrailersSection.kt` | Source Component | 311 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/EpisodeWatchedActionSheet.kt` | Bottom Sheet (Compose) | 232 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/ExpandableDescription.kt` | Source Component | 68 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/HeroTrailerPlayerSurface.kt` | Player Component | 16 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/TabletDetailHero.kt` | Source Component | 438 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/TrailerPlayerPopup.kt` | Player Component | 178 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadSubtitles.kt` | Source Component | 109 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsClock.kt` | Source Component | 5 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsLiveStatusPlatform.kt` | Source Component | 5 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsModels.kt` | Data Model | 114 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsPlatformDownloader.kt` | Source Component | 33 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsRepository.kt` | Repository | 574 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsScreen.kt` | Screen UI (Compose) | 531 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsStorage.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/CachedNextUpRelease.kt` | Source Component | 17 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/HomeCatalogDefinitions.kt` | Source Component | 150 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/HomeCatalogParser.kt` | Parser | 91 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/HomeCatalogSettingsRepository.kt` | Repository | 647 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/HomeCatalogSettingsStorage.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/HomeCatalogSettingsSyncService.kt` | Android Service / API | 235 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/HomeEpisodeShuffle.kt` | Source Component | 126 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/HomeModels.kt` | Data Model | 74 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/HomeRepository.kt` | Repository | 483 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/HomeScreen.kt` | Screen UI (Compose) | 2016 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/HomeScrollPosition.kt` | Source Component | 38 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/ReleaseInfoUtils.kt` | Source Component | 42 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/components/CollectionCardRemoteImage.kt` | Source Component | 14 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/components/HomeCatalogSection.kt` | Source Component | 94 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/components/HomeCollectionRowSection.kt` | Source Component | 215 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/components/HomeContinueWatchingSection.kt` | Source Component | 1320 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/components/HomeHeroSection.kt` | Source Component | 674 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/components/HomePosterCard.kt` | Source Component | 56 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/components/HomeSectionLayout.kt` | Source Component | 12 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/components/HomeSkeletonLoading.kt` | Source Component | 169 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/components/HomeStateCards.kt` | Source Component | 41 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/home/components/PosterGrid.kt` | Source Component | 192 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryClock.kt` | Source Component | 5 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryDisplaySettings.kt` | Source Component | 284 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryDisplaySettingsStorage.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryDownloadsButton.kt` | Source Component | 99 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryListEditor.kt` | Source Component | 139 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryListManagementButton.kt` | Source Component | 62 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryListManagementController.kt` | Source Component | 95 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryListManagementDialog.kt` | Dialog UI (Compose) | 178 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryListRows.kt` | Source Component | 69 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryLocalState.kt` | Source Component | 402 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryModels.kt` | Data Model | 117 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryProviderOrders.kt` | Source Component | 42 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryRepository.kt` | Repository | 777 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibrarySavedContent.kt` | Source Component | 183 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryScreen.kt` | Screen UI (Compose) | 1369 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryServerContent.kt` | Source Component | 96 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibrarySortEffect.kt` | Source Component | 37 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryStorage.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibraryStoragePayload.kt` | Source Component | 39 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/LibrarySyncReconciler.kt` | Source Component | 70 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/TrackingMembershipFeedback.kt` | Source Component | 36 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/TrackingMembershipRemovalConfirmation.kt` | Source Component | 128 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/sync/LibrarySyncAdapter.kt` | Source Component | 48 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/sync/LibrarySyncPaging.kt` | Source Component | 52 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/library/sync/SupabaseLibrarySyncAdapter.kt` | Source Component | 196 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListAccountController.kt` | Source Component | 118 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListApiClient.kt` | Source Component | 89 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListAuthModels.kt` | Data Model | 96 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListAuthRepository.kt` | Repository | 189 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListAuthStore.kt` | Source Component | 137 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListHistoryPayload.kt` | Source Component | 98 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListHistoryService.kt` | Android Service / API | 71 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListHttpClient.kt` | Source Component | 76 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListHttpModels.kt` | Data Model | 52 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListLibraryDecoder.kt` | Source Component | 80 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListLibraryModels.kt` | Data Model | 96 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListLibraryMutation.kt` | Source Component | 50 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListLibraryProjection.kt` | Source Component | 62 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListLibraryRemote.kt` | Source Component | 70 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListLibraryService.kt` | Android Service / API | 187 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListLibrarySorter.kt` | Source Component | 81 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListLibraryWriter.kt` | Source Component | 115 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListMediaIndex.kt` | Source Component | 34 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListMessages.kt` | Source Component | 33 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListMetadataService.kt` | Android Service / API | 80 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListMutationTarget.kt` | Source Component | 93 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListNetworkEngine.kt` | Source Component | 53 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListPlatform.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListProgressProjection.kt` | Source Component | 117 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListRatingsClient.kt` | Source Component | 67 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListRatingsDecoder.kt` | Source Component | 82 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListRatingsRepository.kt` | Repository | 119 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListResponseValues.kt` | Source Component | 97 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListScrobbleReceipt.kt` | Source Component | 71 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListScrobbleService.kt` | Android Service / API | 41 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListSettings.kt` | Source Component | 48 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListSettingsRepository.kt` | Repository | 135 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListSettingsStorage.kt` | Source Component | 28 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListSyncDecoders.kt` | Source Component | 114 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListSyncEngine.kt` | Source Component | 81 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListSyncModels.kt` | Data Model | 142 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListSyncRemote.kt` | Source Component | 63 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListSyncRepository.kt` | Repository | 189 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListSyncState.kt` | Source Component | 27 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListSyncStorage.kt` | Source Component | 14 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListTracker.kt` | Source Component | 91 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListTrackingLibraryProvider.kt` | Source Component | 44 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListTrackingProgressProvider.kt` | Source Component | 55 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListTrackingWrites.kt` | Source Component | 33 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListWatchedDecoder.kt` | Source Component | 64 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListWatchedSyncAdapter.kt` | Source Component | 59 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/MdbListWriteReconciliation.kt` | Source Component | 29 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/mdblist/RottenTomatoesRating.kt` | Source Component | 29 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/membership/MemberAccess.kt` | Source Component | 37 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/membership/MemberAccessRemoteDataSource.kt` | Source Component | 29 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/membership/MemberAccessRepository.kt` | Repository | 169 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/membership/MemberAssetStorage.kt` | Source Component | 13 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/membership/MembershipOverview.kt` | Source Component | 34 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/membership/MembershipOverviewRemoteDataSource.kt` | Source Component | 71 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/membership/MembershipOverviewRepository.kt` | Repository | 81 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/membership/ProfileBackground.kt` | Source Component | 22 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/membership/ProfileBackgroundRepository.kt` | Repository | 236 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/membership/ThemeAccess.kt` | Source Component | 41 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/notifications/EpisodeReleaseNotificationPlatform.kt` | Source Component | 9 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/notifications/EpisodeReleaseNotificationsClock.kt` | Source Component | 5 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/notifications/EpisodeReleaseNotificationsModels.kt` | Data Model | 105 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/notifications/EpisodeReleaseNotificationsRepository.kt` | Repository | 474 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/notifications/EpisodeReleaseNotificationsStorage.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/p2p/P2pConsentDialog.kt` | Dialog UI (Compose) | 52 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/p2p/P2pPlayerOverlays.kt` | Player Component | 132 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/p2p/P2pStreaming.kt` | Source Component | 257 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/AddonSubtitleLoader.kt` | Source Component | 103 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/AudioTrackModal.kt` | Source Component | 156 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/ClickableIncludingFlingStop.kt` | Source Component | 58 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/ExternalPlaybackProgress.kt` | Source Component | 76 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/ExternalPlayerLaunchCoordinator.kt` | Player Component | 153 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/ExternalPlayerLauncherEffect.kt` | Player Component | 21 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/ExternalPlayerPlatform.kt` | Player Component | 71 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/InfusePlayback.kt` | Source Component | 110 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/IosVideoSettingsModal.kt` | Source Component | 285 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/OpeningOverlay.kt` | Source Component | 271 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/ParentalGuideOverlay.kt` | Source Component | 144 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/ParentalGuideRepository.kt` | Repository | 175 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlatformExoPlayback.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlaybackBufferSettings.kt` | Source Component | 56 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerAutoSkipNotification.kt` | Player Component | 19 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerControlActions.kt` | Player Component | 259 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerControls.kt` | Player Component | 936 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerEngine.kt` | Player Component | 88 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerEpisodesPanel.kt` | Player Component | 496 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerGestureOverlay.kt` | Player Component | 193 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerLanguagePreferences.kt` | Player Component | 648 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerLayout.kt` | Player Component | 174 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerLoadingStatus.kt` | Player Component | 53 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerMediaInfo.kt` | Player Component | 36 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerModels.kt` | Data Model | 241 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerNextEpisodeAutoPlay.kt` | Player Component | 364 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerOverlayScaffold.kt` | Player Component | 76 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerOverlays.kt` | Player Component | 333 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerPlatformEffects.kt` | Player Component | 67 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerPlaybackOverlays.kt` | Player Component | 154 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreen.kt` | Screen UI (Compose) | 87 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenArgs.kt` | Screen UI (Compose) | 43 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenContent.kt` | Screen UI (Compose) | 157 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenModalHosts.kt` | Screen UI (Compose) | 240 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenRuntimeAudioPreferences.kt` | Screen UI (Compose) | 60 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenRuntimeEffects.kt` | Screen UI (Compose) | 902 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenRuntimeGestureActions.kt` | Screen UI (Compose) | 325 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenRuntimePlaybackActions.kt` | Screen UI (Compose) | 450 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenRuntimeServerTracks.kt` | Screen UI (Compose) | 127 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenRuntimeSourceActions.kt` | Screen UI (Compose) | 608 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenRuntimeState.kt` | Screen UI (Compose) | 223 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenRuntimeSubtitleActions.kt` | Screen UI (Compose) | 69 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenRuntimeTrackActions.kt` | Screen UI (Compose) | 362 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenRuntimeUi.kt` | Screen UI (Compose) | 675 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerScreenSupport.kt` | Screen UI (Compose) | 89 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerSettingsRepository.kt` | Repository | 1243 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerSettingsStorage.kt` | Player Component | 187 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerSidePanel.kt` | Player Component | 277 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerSourcesPanel.kt` | Player Component | 125 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerStartupSubtitles.kt` | Player Component | 32 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerStreamList.kt` | Player Component | 124 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerStreamsRepository.kt` | Repository | 569 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerSubtitleCueParser.kt` | Parser | 339 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerSubtitleMatching.kt` | Player Component | 387 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerSurfaceGestures.kt` | Player Component | 207 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerTimeline.kt` | Player Component | 185 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerTrackPreferenceStorage.kt` | Player Component | 28 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/PlayerTrackSelection.kt` | Player Component | 569 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/StreamInfoOverlay.kt` | Source Component | 269 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/SubtitleAudioModels.kt` | Data Model | 153 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/SubtitleCacheProvider.kt` | Source Component | 21 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/SubtitleForwarder.kt` | Source Component | 53 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/SubtitleModal.kt` | Source Component | 544 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/SubtitleRepository.kt` | Repository | 75 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/SubtitleSdhFilter.kt` | Source Component | 22 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/SubtitleSelectionModel.kt` | Data Model | 204 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/SubtitleStylePanel.kt` | Source Component | 491 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/VodCacheSizing.kt` | Source Component | 57 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/skip/AutoSkipSegmentType.kt` | Source Component | 34 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/skip/InternalSkipAction.kt` | Source Component | 41 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/skip/NextEpisodeCard.kt` | Source Component | 255 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/skip/PlayerNextEpisodeRules.kt` | Player Component | 180 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/skip/SimklIdResolver.kt` | Stream / Content Resolver | 210 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/skip/SkipIntroApi.kt` | Source Component | 141 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/skip/SkipIntroButton.kt` | Source Component | 169 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/skip/SkipIntroRepository.kt` | Repository | 353 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/skip/SkipModels.kt` | Data Model | 142 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/player/skip/SubmitIntroDialog.kt` | Dialog UI (Compose) | 384 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/plugins/PluginContentIds.kt` | Source Component | 24 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/plugins/PluginModels.kt` | Data Model | 218 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/plugins/PluginRepository.kt` | Repository | 43 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/AvatarPicker.kt` | Source Component | 89 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/AvatarRepository.kt` | Repository | 252 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/AvatarStorage.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/PinEntryDialog.kt` | Dialog UI (Compose) | 267 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileBackgroundBackdrop.kt` | Source Component | 84 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileBackgroundPicker.kt` | Source Component | 176 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileEditScreen.kt` | Screen UI (Compose) | 597 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileHoverHapticFeedback.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileModels.kt` | Data Model | 111 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfilePinCache.kt` | Source Component | 20 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfilePinCacheStorage.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfilePinCrypto.kt` | Source Component | 5 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfilePinEntry.kt` | Source Component | 238 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfilePopupContent.kt` | Source Component | 236 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfilePopupPositionProvider.kt` | Source Component | 44 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileRepository.kt` | Repository | 564 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileSelectionRouting.kt` | Source Component | 27 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileSelectionScreen.kt` | Screen UI (Compose) | 559 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileStorage.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/profiles/ProfileSwitcherTab.kt` | Source Component | 815 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/DiscoverSelectionStorage.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchDiscoverContent.kt` | Source Component | 262 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchHistoryRepository.kt` | Repository | 111 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchHistoryStorage.kt` | Source Component | 8 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchModels.kt` | Data Model | 62 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchRepository.kt` | Repository | 705 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/search/SearchScreen.kt` | Screen UI (Compose) | 527 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/ServerCatalog.kt` | Source Component | 220 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/ServerMatcher.kt` | Source Component | 220 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/ServerMessages.kt` | Source Component | 45 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/ServerModels.kt` | Data Model | 212 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/ServerPlayback.kt` | Source Component | 229 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/ServerProvider.kt` | Source Component | 94 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/ServerRepository.kt` | Repository | 364 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/ServerStorage.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/ServerStreams.kt` | Source Component | 103 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/ServerSync.kt` | Source Component | 107 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/ServerUserStateProjection.kt` | Source Component | 71 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/ServerWatched.kt` | Source Component | 144 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/emby/EmbyProvider.kt` | Source Component | 31 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/jellyfin/JellyfinProvider.kt` | Source Component | 27 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/mediabrowser/MediaBrowserClient.kt` | Source Component | 152 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/mediabrowser/MediaBrowserMapper.kt` | Source Component | 228 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/mediabrowser/MediaBrowserModels.kt` | Data Model | 203 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/servers/mediabrowser/MediaBrowserProvider.kt` | Source Component | 601 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/AccountSettingsPage.kt` | Source Component | 252 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/AddonsSettingsPage.kt` | Source Component | 14 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/AdvancedSettingsPage.kt` | Source Component | 240 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/AppBrandWordmark.kt` | Source Component | 32 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/AppIconOption.kt` | Source Component | 37 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/AppIconPicker.kt` | Source Component | 331 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/AppIconPlatform.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/AppIconRepository.kt` | Repository | 52 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/AppIconResources.kt` | Source Component | 46 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/AppLanguage.kt` | Source Component | 76 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/AppearanceSettingsPage.kt` | Source Component | 366 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/AppearanceThemePicker.kt` | Source Component | 169 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/AutoSkipSegmentSelectionDialog.kt` | Dialog UI (Compose) | 84 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/ContentDiscoverySettingsPage.kt` | Source Component | 87 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/ContinueWatchingSettingsPage.kt` | Source Component | 358 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/CustomThemeEditor.kt` | Source Component | 254 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/CustomThemePreview.kt` | Source Component | 59 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/DebridSettingsPage.kt` | Source Component | 1712 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/HomescreenSettingsPage.kt` | Source Component | 374 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/IntegrationLogoPainter.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/IntegrationsSettingsPage.kt` | Source Component | 51 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/LicensesAttributionsPage.kt` | Source Component | 413 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/MdbListLibraryListsPicker.kt` | Source Component | 243 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/MdbListProviderCard.kt` | Source Component | 87 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/MdbListSettingsPage.kt` | Source Component | 220 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/MediaServerSettingsPage.kt` | Source Component | 303 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/MediaServersSettingsPage.kt` | Source Component | 122 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/MemberBrandWordmark.kt` | Source Component | 199 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/MetaScreenSettingsPage.kt` | Screen UI (Compose) | 670 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/NavBarStyle.kt` | Source Component | 24 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/NavigationBarPreview.kt` | Source Component | 121 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/NavigationBarSettingsSheet.kt` | Bottom Sheet (Compose) | 103 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/NotificationsSettingsPage.kt` | Source Component | 159 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/PlaybackSettingsPage.kt` | Source Component | 2919 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/PluginsSettingsPage.kt` | Source Component | 5 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/PosterCustomizationSettingsPage.kt` | Source Component | 969 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/RatingsSettings.kt` | Source Component | 54 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/SentrySettingsRepository.kt` | Repository | 32 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/SentrySettingsStorage.kt` | Source Component | 10 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/ServerSignInSheet.kt` | Bottom Sheet (Compose) | 265 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/SettingsComponents.kt` | Source Component | 573 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/SettingsFullScreenPages.kt` | Screen UI (Compose) | 207 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/SettingsModels.kt` | Data Model | 176 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/SettingsRootPage.kt` | Source Component | 288 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/SettingsScreen.kt` | Screen UI (Compose) | 1298 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/SettingsSearch.kt` | Source Component | 1232 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/SettingsSecretTextField.kt` | Source Component | 81 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/SimklSyncInfoDialog.kt` | Dialog UI (Compose) | 75 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/StreamsSettingsPage.kt` | Source Component | 670 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/SupporterMembershipCard.kt` | Source Component | 507 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/SupportersContributorsPage.kt` | Source Component | 975 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/ThemeColorPicker.kt` | Source Component | 145 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/ThemeSettingsRepository.kt` | Repository | 181 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/ThemeSettingsStorage.kt` | Source Component | 23 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/TmdbSettingsPage.kt` | Source Component | 343 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/TrackingAdaptivePicker.kt` | Source Component | 260 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/TrackingProviderCards.kt` | Source Component | 815 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/TrackingSettingsPage.kt` | Source Component | 751 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/UpdateChannelSettings.kt` | Source Component | 115 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/shuffle/EpisodeShuffle.kt` | Source Component | 71 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/shuffle/EpisodeShuffleRepository.kt` | Repository | 73 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/shuffle/EpisodeShuffleSheet.kt` | Bottom Sheet (Compose) | 241 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/shuffle/RandomEpisodePicker.kt` | Source Component | 69 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/shuffle/ShuffleBadge.kt` | Source Component | 23 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/shuffle/ShuffleEpisodeState.kt` | Source Component | 63 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklAnimeIdPreference.kt` | Source Component | 33 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklAnimeWatchedFallback.kt` | Source Component | 37 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklApiClient.kt` | Source Component | 324 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklApiMetadata.kt` | Source Component | 47 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklApplicationAdapters.kt` | Source Component | 292 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklAuthModels.kt` | Data Model | 72 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklAuthParsing.kt` | Source Component | 64 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklAuthRepository.kt` | Repository | 427 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklBrandPainter.kt` | Source Component | 12 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklLibraryAdapter.kt` | Source Component | 275 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklLibraryProjection.kt` | Source Component | 127 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklMutationReceipt.kt` | Source Component | 290 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklMutationReconciliation.kt` | Source Component | 157 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklMutationRepository.kt` | Repository | 499 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklPkce.kt` | Source Component | 51 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklPlatform.kt` | Source Component | 20 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklPlaybackMerge.kt` | Source Component | 50 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklPlaybackReconciliation.kt` | Source Component | 130 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklProjections.kt` | Source Component | 634 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklRefreshPolicy.kt` | Source Component | 26 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklRelatedRepository.kt` | Repository | 197 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklScrobbleReconciliation.kt` | Source Component | 266 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklScrobbleResult.kt` | Source Component | 153 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklSyncEngine.kt` | Source Component | 137 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklSyncModels.kt` | Data Model | 204 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklSyncRemote.kt` | Source Component | 99 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklSyncRepository.kt` | Repository | 267 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklSyncStorage.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/simkl/SimklWatchDiagnostics.kt` | Source Component | 127 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/BadgeImageLoader.kt` | Source Component | 35 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/BingeGroupCacheRepository.kt` | Repository | 21 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/BingeGroupCacheStorage.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/PlaybackAvailability.kt` | Source Component | 90 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/PlaybackUrlCredentials.kt` | Source Component | 71 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/ProviderFilterRow.kt` | Source Component | 215 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamAutoPlayLoadingPolicy.kt` | Source Component | 37 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamAutoPlayModels.kt` | Data Model | 13 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamAutoPlayPolicy.kt` | Source Component | 22 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamAutoPlaySelector.kt` | Source Component | 247 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamBadgeChip.kt` | Source Component | 182 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamBadgePresentation.kt` | Source Component | 29 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamBadgeRules.kt` | Source Component | 308 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamBadgeSettingsRepository.kt` | Repository | 263 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamBadgeSettingsStorage.kt` | Source Component | 20 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamCard.kt` | Source Component | 291 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamContextStore.kt` | Source Component | 27 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamFetchSupport.kt` | Source Component | 156 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamLaunchStore.kt` | Source Component | 44 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamLinkCacheRepository.kt` | Repository | 121 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamLinkCacheStorage.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamModels.kt` | Data Model | 337 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamParser.kt` | Parser | 180 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamsRepository.kt` | Repository | 677 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamsScreen.kt` | Screen UI (Compose) | 1186 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/StreamsTabletLayout.kt` | Source Component | 404 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/streams/YouTubeStreamResolver.kt` | Stream / Content Resolver | 40 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tmdb/TmdbAgeRatings.kt` | Source Component | 23 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tmdb/TmdbImages.kt` | Source Component | 42 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tmdb/TmdbMetadataService.kt` | Android Service / API | 2236 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tmdb/TmdbService.kt` | Android Service / API | 156 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tmdb/TmdbSettings.kt` | Source Component | 18 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tmdb/TmdbSettingsRepository.kt` | Repository | 217 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tmdb/TmdbSettingsStorage.kt` | Source Component | 36 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tracking/TrackingAttribution.kt` | Source Component | 35 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tracking/TrackingLibraryMembership.kt` | Source Component | 31 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tracking/TrackingLibrarySorter.kt` | Source Component | 11 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tracking/TrackingListManager.kt` | Source Component | 19 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tracking/TrackingMedia.kt` | Source Component | 171 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tracking/TrackingProvider.kt` | Source Component | 256 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tracking/TrackingReads.kt` | Source Component | 165 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tracking/TrackingRefreshGate.kt` | Source Component | 42 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tracking/TrackingScrobbleCoordinator.kt` | Source Component | 93 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tracking/TrackingSettings.kt` | Source Component | 43 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tracking/TrackingSources.kt` | Source Component | 57 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/tracking/TrackingWrites.kt` | Source Component | 94 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trailer/LetterboxDetector.kt` | Source Component | 77 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trailer/TrailerPlaybackResolver.kt` | Stream / Content Resolver | 8 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trailer/TrailerPlaybackSource.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trailer/TrailerPlaybackState.kt` | Source Component | 47 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trailer/TrailerPlayer.kt` | Player Component | 139 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trailer/TrailerPlayerControls.kt` | Player Component | 160 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktAuthRepository.kt` | Repository | 604 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktAuthStorage.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktBrandPainter.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktCommentsModels.kt` | Data Model | 77 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktCommentsRepository.kt` | Repository | 248 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktCommentsSettings.kt` | Source Component | 33 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktCommentsStorage.kt` | Source Component | 10 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktEpisodeMappingService.kt` | Android Service / API | 655 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktIdUtils.kt` | Source Component | 102 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktImageUtils.kt` | Source Component | 60 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktIsoDateParser.kt` | Parser | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktLibraryRepository.kt` | Repository | 935 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktLibraryStorage.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktModels.kt` | Data Model | 81 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktPlatformClock.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktProgressRepository.kt` | Repository | 1661 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktPublicListSourceResolver.kt` | Stream / Content Resolver | 400 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktRelatedRepository.kt` | Repository | 327 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktScrobbleRepository.kt` | Repository | 458 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktSettingsRepository.kt` | Repository | 232 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktSettingsStorage.kt` | Source Component | 9 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktTrackingLibraryProvider.kt` | Source Component | 110 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktTrackingProgressProvider.kt` | Source Component | 144 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/TraktWatchedShowSnapshotRepository.kt` | Repository | 267 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/trakt/WatchProgressSourceSettingsOutbox.kt` | Source Component | 86 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/updater/AppUpdater.kt` | Source Component | 358 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/updater/AppUpdaterBanner.kt` | Source Component | 389 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/updater/AppUpdaterPlatform.kt` | Source Component | 30 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/updater/AppUpdaterRepository.kt` | Repository | 105 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/updater/ReleaseSelector.kt` | Source Component | 43 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/updater/UpdateChannel.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/updater/UpdatePreferences.kt` | Source Component | 28 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/updater/VersionUtils.kt` | Source Component | 78 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watched/StoredWatchedPayload.kt` | Source Component | 227 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watched/WatchedBadgeBulkResolver.kt` | Stream / Content Resolver | 179 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watched/WatchedClock.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watched/WatchedEpisodeActions.kt` | Source Component | 105 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watched/WatchedItemsStore.kt` | Source Component | 35 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watched/WatchedModels.kt` | Data Model | 118 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watched/WatchedRepository.kt` | Repository | 1423 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watched/WatchedStorage.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watching/application/WatchingActions.kt` | Source Component | 209 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watching/application/WatchingState.kt` | Source Component | 130 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watching/domain/SeriesContinuity.kt` | Source Component | 227 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watching/domain/WatchingModels.kt` | Data Model | 58 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watching/domain/WatchingPolicies.kt` | Source Component | 218 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watching/sync/ProgressSyncAdapter.kt` | Source Component | 55 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watching/sync/SupabaseProgressSyncAdapter.kt` | Source Component | 156 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watching/sync/SupabaseWatchedSyncAdapter.kt` | Source Component | 158 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watching/sync/TraktWatchedProjection.kt` | Source Component | 63 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watching/sync/TraktWatchedSyncAdapter.kt` | Source Component | 758 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watching/sync/WatchedSyncAdapter.kt` | Source Component | 57 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/AirDateUtils.kt` | Source Component | 81 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/ContinueWatchingEnrichmentCache.kt` | Source Component | 217 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/ContinueWatchingEnrichmentStorage.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/ContinueWatchingPreferencesRepository.kt` | Repository | 196 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/ContinueWatchingPreferencesStorage.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/CurrentDateProvider.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/ResumePromptRepository.kt` | Repository | 70 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/ResumePromptStorage.kt` | Source Component | 8 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/TrackingProviderRefreshCoordinator.kt` | Source Component | 14 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/WatchProgressClock.kt` | Source Component | 5 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/WatchProgressIdentity.kt` | Source Component | 190 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/WatchProgressMetadataProjection.kt` | Source Component | 92 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/WatchProgressModels.kt` | Data Model | 387 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/WatchProgressRepository.kt` | Repository | 1751 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/WatchProgressRules.kt` | Source Component | 206 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/WatchProgressSourceCoordinator.kt` | Source Component | 508 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/WatchProgressSourceProjection.kt` | Source Component | 13 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/watchprogress/WatchProgressStorage.kt` | Source Component | 6 | DOCUMENTED |  |

### Flavor: Full Common (Plugins/Runtime)

| File Path | Type / Role | Lines | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/plugins/PluginManifestParser.kt` | Parser | 29 | DOCUMENTED |  |
| `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/plugins/PluginRepository.kt` | Repository | 667 | DOCUMENTED |  |
| `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/plugins/PluginSettingsDialog.kt` | Dialog UI (Compose) | 196 | DOCUMENTED |  |
| `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/plugins/PluginsSettingsScreen.kt` | Screen UI (Compose) | 526 | DOCUMENTED |  |
| `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/plugins/runtime/PluginRuntime.kt` | Runtime Engine | 281 | DOCUMENTED |  |
| `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/plugins/runtime/crypto/CryptoBridge.kt` | Source Component | 135 | DOCUMENTED |  |
| `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/plugins/runtime/dom/DomBridge.kt` | Source Component | 135 | DOCUMENTED |  |
| `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/plugins/runtime/host/HostApiRegistry.kt` | Source Component | 19 | DOCUMENTED |  |
| `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/plugins/runtime/host/HostFunctions.kt` | Source Component | 63 | DOCUMENTED |  |
| `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/plugins/runtime/js/JsBindings.kt` | JS/Native Bindings | 1120 | DOCUMENTED |  |
| `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/plugins/runtime/js/JsRuntime.kt` | Runtime Engine | 48 | DOCUMENTED |  |
| `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/plugins/runtime/network/FetchBridge.kt` | Source Component | 117 | DOCUMENTED |  |
| `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/plugins/runtime/network/UrlBridge.kt` | Source Component | 53 | DOCUMENTED |  |
| `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/plugins/runtime/wasm/WasmBridge.kt` | Source Component | 16 | DOCUMENTED |  |
| `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/settings/PluginsSettingsPage.kt` | Source Component | 14 | DOCUMENTED |  |
| `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/trailer/InAppYouTubeExtractor.kt` | Source Component | 709 | DOCUMENTED |  |
| `composeApp/src/fullCommonMain/kotlin/com/nuvio/app/features/trailer/TrailerPlaybackResolver.kt` | Stream / Content Resolver | 15 | DOCUMENTED |  |

### Flavor: Android Full

| File Path | Type / Role | Lines | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `composeApp/src/androidFull/AndroidManifest.xml` | XML Config / Resource | 6 | DOCUMENTED |  |
| `composeApp/src/androidFull/kotlin/com/nuvio/app/features/details/components/HeroTrailerPlayerSurface.android.kt` | Player Component | 289 | DOCUMENTED |  |
| `composeApp/src/androidFull/kotlin/com/nuvio/app/features/player/PlatformPlaybackDataSourceFactory.android.kt` | Source Component | 39 | DOCUMENTED |  |
| `composeApp/src/androidFull/kotlin/com/nuvio/app/features/plugins/PluginCrypto.android.kt` | Source Component | 273 | DOCUMENTED |  |
| `composeApp/src/androidFull/kotlin/com/nuvio/app/features/plugins/PluginPlatform.android.kt` | Source Component | 55 | DOCUMENTED |  |
| `composeApp/src/androidFull/kotlin/com/nuvio/app/features/plugins/PluginScraperCodeFileStore.android.kt` | Source Component | 64 | DOCUMENTED |  |
| `composeApp/src/androidFull/kotlin/com/nuvio/app/features/plugins/runtime/PluginRuntime.android.kt` | Runtime Engine | 21 | DOCUMENTED |  |
| `composeApp/src/androidFull/kotlin/com/nuvio/app/features/trailer/LetterboxSampler.kt` | Source Component | 25 | DOCUMENTED |  |
| `composeApp/src/androidFull/kotlin/com/nuvio/app/features/trailer/TrailerExtractionPlatform.android.kt` | Source Component | 176 | DOCUMENTED |  |
| `composeApp/src/androidFull/kotlin/com/nuvio/app/features/trailer/YoutubeChunkedDataSourceFactory.kt` | Source Component | 153 | DOCUMENTED |  |
| `composeApp/src/androidFull/kotlin/com/nuvio/app/features/updater/AndroidAppUpdaterPlatform.kt` | Source Component | 165 | DOCUMENTED |  |
| `composeApp/src/androidFull/kotlin/com/nuvio/app/features/updater/AppUpdaterPlatform.android.kt` | Source Component | 39 | DOCUMENTED |  |
| `composeApp/src/androidFull/res/xml/updater_file_paths.xml` | XML Config / Resource | 6 | DOCUMENTED |  |

### Flavor: Android Playstore Stubs

| File Path | Type / Role | Lines | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `composeApp/src/androidPlaystore/kotlin/com/nuvio/app/features/details/components/HeroTrailerPlayerSurface.android.kt` | Player Component | 21 | DOCUMENTED |  |
| `composeApp/src/androidPlaystore/kotlin/com/nuvio/app/features/player/PlatformPlaybackDataSourceFactory.android.kt` | Source Component | 34 | DOCUMENTED |  |
| `composeApp/src/androidPlaystore/kotlin/com/nuvio/app/features/plugins/PluginPlatform.android.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/androidPlaystore/kotlin/com/nuvio/app/features/plugins/PluginRepository.android.kt` | Repository | 53 | DOCUMENTED |  |
| `composeApp/src/androidPlaystore/kotlin/com/nuvio/app/features/settings/PluginsSettingsPage.kt` | Source Component | 5 | DOCUMENTED |  |
| `composeApp/src/androidPlaystore/kotlin/com/nuvio/app/features/trailer/TrailerPlaybackResolver.android.kt` | Stream / Content Resolver | 7 | DOCUMENTED |  |
| `composeApp/src/androidPlaystore/kotlin/com/nuvio/app/features/updater/AndroidAppUpdaterPlatform.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/androidPlaystore/kotlin/com/nuvio/app/features/updater/AppUpdaterPlatform.android.kt` | Source Component | 36 | DOCUMENTED |  |

### Android Main: Core / Platform

| File Path | Type / Role | Lines | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/MainActivity.kt` | Source Component | 201 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/NuvioApplication.kt` | Source Component | 45 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/Platform.android.kt` | Source Component | 13 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/auth/AuthStorage.android.kt` | Source Component | 26 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/auth/DeviceSessionRegistration.android.kt` | Source Component | 19 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/diagnostics/SentryInitializer.kt` | Source Component | 106 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/diagnostics/SentryNetworkBreadcrumbInterceptor.kt` | Source Component | 91 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/format/LocalDateTimeDisplay.android.kt` | Source Component | 10 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/format/ReleaseDateDisplay.android.kt` | Source Component | 12 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/network/ApiHttpClient.android.kt` | Source Component | 22 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/network/IPv4FirstDns.kt` | Source Component | 16 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/network/ServerConfigurationStorage.android.kt` | Configuration | 53 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/poster/CustomPosterUrlStorage.android.kt` | Source Component | 43 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/storage/PlatformLocalAccountDataCleaner.android.kt` | Source Component | 60 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/sync/AppForegroundMonitor.android.kt` | Source Component | 33 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/sync/SyncClientIdentityStorage.android.kt` | Source Component | 25 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/time/EpisodeReleaseDatePlatform.android.kt` | Source Component | 23 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/ui/AppIconPainter.android.kt` | Source Component | 21 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/ui/AshSwarmRenderer.android.kt` | Source Component | 93 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/ui/CardDepthStyleStorage.android.kt` | Source Component | 26 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/ui/FloatingNavigationBar.android.kt` | Source Component | 249 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/ui/NativeBottomSheet.android.kt` | Bottom Sheet (Compose) | 23 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/ui/NativeTabBridge.android.kt` | Source Component | 25 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/ui/PlatformBackHandler.android.kt` | Source Component | 12 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/ui/PlatformExitApp.android.kt` | Source Component | 9 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/ui/PlatformImageLoader.android.kt` | Source Component | 17 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/ui/PlatformInsets.android.kt` | Source Component | 19 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/ui/PosterCardStyleStorage.android.kt` | Source Component | 26 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/ui/glass/GlassBarShader.kt` | Source Component | 52 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/ui/glass/GlassBarSurface.kt` | Source Component | 88 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/ui/jelly/JellyDrawing.kt` | Source Component | 68 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/ui/jelly/JellyMotion.kt` | Source Component | 174 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/ui/jelly/JellySpring.kt` | Source Component | 69 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/core/ui/jelly/JellyTabs.kt` | Source Component | 162 | DOCUMENTED |  |

### Android Main: Feature Features

| File Path | Type / Role | Lines | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/addons/AddonPlatform.android.kt` | Source Component | 336 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/collection/CollectionMobileSettingsStorage.android.kt` | Source Component | 26 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/collection/CollectionStorage.android.kt` | Source Component | 26 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/debrid/DebridSettingsStorage.android.kt` | Source Component | 273 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/details/MetaScreenSettingsStorage.android.kt` | Screen UI (Compose) | 26 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/details/SeasonViewModeStorage.android.kt` | Source Component | 26 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/details/components/BackdropPaletteImage.android.kt` | Source Component | 9 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/downloads/AndroidDownloadScheduler.kt` | Source Component | 243 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/downloads/AndroidDownloadStore.kt` | Source Component | 86 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/downloads/AndroidDownloadTransfer.kt` | Source Component | 123 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/downloads/DownloadSubtitleStorage.android.kt` | Source Component | 37 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/downloads/DownloadsClock.android.kt` | Source Component | 5 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/downloads/DownloadsLiveStatusPlatform.android.kt` | Source Component | 298 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/downloads/DownloadsNotificationActionReceiver.kt` | BroadcastReceiver | 44 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/downloads/DownloadsPlatformDownloader.android.kt` | Source Component | 147 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/downloads/DownloadsStorage.android.kt` | Source Component | 26 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/downloads/DownloadsTransferJobService.kt` | Android Service / API | 71 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/downloads/DownloadsTransferWorker.kt` | Source Component | 39 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/home/HomeCatalogSettingsStorage.android.kt` | Source Component | 26 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/home/components/CollectionCardRemoteImage.android.kt` | Source Component | 34 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/library/LibraryClock.android.kt` | Source Component | 5 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/library/LibraryDisplaySettingsStorage.android.kt` | Source Component | 26 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/library/LibraryStorage.android.kt` | Source Component | 25 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/mdblist/MdbListAuthPersistence.android.kt` | Source Component | 77 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/mdblist/MdbListSettingsStorage.android.kt` | Source Component | 155 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/mdblist/MdbListSyncStorage.android.kt` | Source Component | 32 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/membership/MemberAssetStorage.android.kt` | Source Component | 77 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/notifications/EpisodeReleaseNotificationPlatform.android.kt` | Source Component | 309 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/notifications/EpisodeReleaseNotificationWorker.android.kt` | Source Component | 50 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/notifications/EpisodeReleaseNotificationsClock.android.kt` | Source Component | 12 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/notifications/EpisodeReleaseNotificationsStorage.android.kt` | Source Component | 26 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/p2p/P2pSettingsStorage.android.kt` | Source Component | 80 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/p2p/P2pStreamingEngine.android.kt` | Source Component | 857 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/BufferedReadDataSource.kt` | Source Component | 65 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/CustomDefaultTrackNameProvider.kt` | Source Component | 91 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/DeviceLanguagePreferences.android.kt` | Source Component | 34 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/ExternalPlayerActivityContract.kt` | Player Component | 83 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/ExternalPlayerLauncherEffect.android.kt` | Player Component | 40 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/ExternalPlayerPlatform.android.kt` | Player Component | 177 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/NuvioExoPlayerPerformanceHelper.kt` | Player Component | 209 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/PlatformExoPlayback.android.kt` | Source Component | 38 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/PlaybackMediaItems.android.kt` | Source Component | 232 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/PlaybackSubtitleMime.android.kt` | Source Component | 19 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/PlayerEngine.android.kt` | Player Component | 2469 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/PlayerLibassCompat.kt` | Player Component | 116 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/PlayerLibassExtensions.kt` | Player Component | 21 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/PlayerNowPlayingController.android.kt` | Player Component | 512 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/PlayerNowPlayingService.android.kt` | Android Service / API | 174 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/PlayerPictureInPictureManager.android.kt` | Player Component | 210 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/PlayerPlatformEffects.android.kt` | Player Component | 227 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/PlayerPlaybackNetworking.kt` | Player Component | 169 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/PlayerSettingsStorage.android.kt` | Player Component | 1483 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/PlayerSidecarSubtitles.kt` | Player Component | 331 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/PlayerSubtitleRtlFix.kt` | Player Component | 341 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/PlayerSubtitleUtils.kt` | Player Component | 88 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/PlayerTrackPreferenceStorage.android.kt` | Player Component | 127 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/ResponseHeaderOverridingDataSource.kt` | Source Component | 50 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/SubtitleCacheProvider.android.kt` | Source Component | 11 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/SubtitleFileCache.kt` | Source Component | 138 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/VodCache.kt` | Source Component | 281 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/VodCacheWriteSink.kt` | Source Component | 210 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/player/skip/DateComponents.android.kt` | Source Component | 12 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/profiles/AvatarStorage.android.kt` | Source Component | 25 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/profiles/ProfileHoverHapticFeedback.android.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/profiles/ProfilePinCacheStorage.android.kt` | Source Component | 33 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/profiles/ProfilePinCrypto.android.kt` | Source Component | 12 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/profiles/ProfileStorage.android.kt` | Source Component | 25 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/search/DiscoverSelectionStorage.android.kt` | Source Component | 26 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/search/SearchHistoryStorage.android.kt` | Source Component | 40 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/servers/ServerStorage.android.kt` | Source Component | 76 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/settings/AppIconPlatform.android.kt` | Source Component | 117 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/settings/IntegrationLogoPainter.android.kt` | Source Component | 22 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/settings/SentrySettingsStorage.android.kt` | Source Component | 31 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/settings/ThemeSettingsStorage.android.kt` | Source Component | 157 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/shuffle/EpisodeShuffleStorage.android.kt` | Source Component | 21 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/simkl/SimklBrandPainter.android.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/simkl/SimklPlatform.android.kt` | Source Component | 127 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/simkl/SimklSyncStorage.android.kt` | Source Component | 29 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/streams/BingeGroupCacheStorage.android.kt` | Source Component | 32 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/streams/EpochMs.android.kt` | Source Component | 3 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/streams/StreamBadgeSettingsStorage.android.kt` | Source Component | 118 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/streams/StreamLinkCacheStorage.android.kt` | Source Component | 32 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/tmdb/TmdbSettingsStorage.android.kt` | Source Component | 196 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/trakt/TraktAuthStorage.android.kt` | Source Component | 33 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/trakt/TraktBrandPainter.android.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/trakt/TraktCommentsStorage.android.kt` | Source Component | 47 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/trakt/TraktLibraryStorage.android.kt` | Source Component | 26 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/trakt/TraktPlatformClock.android.kt` | Source Component | 13 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/trakt/TraktSettingsStorage.android.kt` | Source Component | 44 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/watched/WatchedClock.android.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/watched/WatchedStorage.android.kt` | Source Component | 26 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/watchprogress/ContinueWatchingEnrichmentStorage.android.kt` | Source Component | 31 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/watchprogress/ContinueWatchingPreferencesStorage.android.kt` | Source Component | 26 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/watchprogress/CurrentDateProvider.android.kt` | Source Component | 16 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/watchprogress/ResumePromptStorage.android.kt` | Source Component | 38 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/watchprogress/WatchProgressClock.android.kt` | Source Component | 5 | DOCUMENTED |  |
| `composeApp/src/androidMain/kotlin/com/nuvio/app/features/watchprogress/WatchProgressStorage.android.kt` | Source Component | 25 | DOCUMENTED |  |

### iOS Platform Implementation

| File Path | Type / Role | Lines | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `composeApp/src/iosAppStore/kotlin/com/nuvio/app/features/details/components/HeroTrailerPlayerSurface.ios.kt` | Player Component | 21 | DOCUMENTED |  |
| `composeApp/src/iosAppStore/kotlin/com/nuvio/app/features/p2p/P2pStreamingEngine.ios.kt` | Source Component | 29 | DOCUMENTED |  |
| `composeApp/src/iosAppStore/kotlin/com/nuvio/app/features/plugins/PluginRepository.ios.kt` | Repository | 53 | DOCUMENTED |  |
| `composeApp/src/iosAppStore/kotlin/com/nuvio/app/features/settings/PluginsSettingsPage.kt` | Source Component | 5 | DOCUMENTED |  |
| `composeApp/src/iosAppStore/kotlin/com/nuvio/app/features/trailer/TrailerPlaybackResolver.ios.kt` | Stream / Content Resolver | 7 | DOCUMENTED |  |
| `composeApp/src/iosFull/kotlin/com/nuvio/app/features/details/components/HeroTrailerPlayerSurface.ios.kt` | Player Component | 16 | DOCUMENTED |  |
| `composeApp/src/iosFull/kotlin/com/nuvio/app/features/p2p/P2pStreamingEngine.ios.kt` | Source Component | 597 | DOCUMENTED |  |
| `composeApp/src/iosFull/kotlin/com/nuvio/app/features/plugins/PluginCrypto.ios.kt` | Source Component | 552 | DOCUMENTED |  |
| `composeApp/src/iosFull/kotlin/com/nuvio/app/features/plugins/PluginPlatform.ios.kt` | Source Component | 145 | DOCUMENTED |  |
| `composeApp/src/iosFull/kotlin/com/nuvio/app/features/plugins/runtime/PluginRuntime.ios.kt` | Runtime Engine | 13 | DOCUMENTED |  |
| `composeApp/src/iosFull/kotlin/com/nuvio/app/features/trailer/TrailerExtractionPlatform.ios.kt` | Source Component | 162 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/MainViewController.kt` | Source Component | 106 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/Platform.ios.kt` | Source Component | 13 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/auth/AuthStorage.ios.kt` | Source Component | 18 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/auth/DeviceSessionRegistration.ios.kt` | Source Component | 17 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/format/LocalDateTimeDisplay.ios.kt` | Source Component | 11 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/format/ReleaseDateDisplay.ios.kt` | Source Component | 19 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/network/ApiHttpClient.ios.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/network/ServerConfigurationStorage.ios.kt` | Configuration | 54 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/poster/CustomPosterUrlStorage.ios.kt` | Source Component | 36 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/storage/PlatformLocalAccountDataCleaner.ios.kt` | Source Component | 110 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/sync/AppForegroundMonitor.ios.kt` | Source Component | 42 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/sync/SyncClientIdentityStorage.ios.kt` | Source Component | 14 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/time/EpisodeReleaseDatePlatform.ios.kt` | Source Component | 29 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/ui/AppIconPainter.ios.kt` | Source Component | 29 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/ui/AshSwarmRenderer.ios.kt` | Source Component | 61 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/ui/CardDepthStyleStorage.ios.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/ui/FloatingNavigationBar.ios.kt` | Source Component | 48 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/ui/NativeBottomSheet.ios.kt` | Bottom Sheet (Compose) | 188 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/ui/NativeTabBridge.ios.kt` | Source Component | 86 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/ui/PlatformBackHandler.ios.kt` | Source Component | 9 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/ui/PlatformExitApp.ios.kt` | Source Component | 3 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/ui/PlatformImageLoader.ios.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/ui/PlatformInsets.ios.kt` | Source Component | 33 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/core/ui/PosterCardStyleStorage.ios.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/addons/AddonPlatform.ios.kt` | Source Component | 199 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/collection/CollectionMobileSettingsStorage.ios.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/collection/CollectionStorage.ios.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/debrid/DebridSettingsStorage.ios.kt` | Source Component | 255 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/details/MetaScreenSettingsStorage.ios.kt` | Screen UI (Compose) | 15 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/details/SeasonViewModeStorage.ios.kt` | Source Component | 19 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/details/components/BackdropPaletteImage.ios.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/downloads/DownloadSubtitleStorage.ios.kt` | Source Component | 36 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/downloads/DownloadsClock.ios.kt` | Source Component | 9 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/downloads/DownloadsLiveStatusPlatform.ios.kt` | Source Component | 82 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/downloads/DownloadsPlatformDownloader.ios.kt` | Source Component | 517 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/downloads/DownloadsStorage.ios.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/home/HomeCatalogSettingsStorage.ios.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/home/components/CollectionCardRemoteImage.ios.kt` | Source Component | 354 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/library/LibraryClock.ios.kt` | Source Component | 9 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/library/LibraryDisplaySettingsStorage.ios.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/library/LibraryStorage.ios.kt` | Source Component | 14 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/mdblist/MdbListAuthPersistence.ios.kt` | Source Component | 86 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/mdblist/MdbListSettingsStorage.ios.kt` | Source Component | 141 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/mdblist/MdbListSyncStorage.ios.kt` | Source Component | 24 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/membership/MemberAssetStorage.ios.kt` | Source Component | 114 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/notifications/EpisodeReleaseNotificationPlatform.ios.kt` | Source Component | 215 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/notifications/EpisodeReleaseNotificationsClock.ios.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/notifications/EpisodeReleaseNotificationsStorage.ios.kt` | Source Component | 18 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/p2p/P2pSettingsStorage.ios.kt` | Source Component | 62 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/player/DeviceLanguagePreferences.ios.kt` | Source Component | 26 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/player/ExternalPlayerLauncherEffect.ios.kt` | Player Component | 49 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/player/ExternalPlayerPlatform.ios.kt` | Player Component | 130 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/player/PlatformExoPlayback.ios.kt` | Source Component | 11 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/player/PlayerBridge.kt` | Player Component | 112 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/player/PlayerEngine.ios.kt` | Player Component | 476 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/player/PlayerPlatformEffects.ios.kt` | Player Component | 138 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/player/PlayerSettingsStorage.ios.kt` | Player Component | 1239 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/player/PlayerTrackPreferenceStorage.ios.kt` | Player Component | 122 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/player/SubtitleCacheProvider.ios.kt` | Source Component | 12 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/player/skip/DateComponents.ios.kt` | Source Component | 20 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/profiles/AvatarStorage.ios.kt` | Source Component | 14 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/profiles/ProfileHoverHapticFeedback.ios.kt` | Source Component | 23 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/profiles/ProfilePinCacheStorage.ios.kt` | Source Component | 18 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/profiles/ProfilePinCrypto.ios.kt` | Source Component | 18 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/profiles/ProfileStorage.ios.kt` | Source Component | 14 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/search/DiscoverSelectionStorage.ios.kt` | Source Component | 18 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/search/SearchHistoryStorage.ios.kt` | Source Component | 26 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/servers/ServerStorage.ios.kt` | Source Component | 87 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/settings/AppIconPlatform.ios.kt` | Source Component | 55 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/settings/IntegrationLogoPainter.ios.kt` | Source Component | 22 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/settings/SentrySettingsStorage.ios.kt` | Source Component | 24 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/settings/ThemeSettingsStorage.ios.kt` | Source Component | 147 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/shuffle/EpisodeShuffleStorage.ios.kt` | Source Component | 13 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/simkl/SimklBrandPainter.ios.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/simkl/SimklPlatform.ios.kt` | Source Component | 167 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/simkl/SimklSyncStorage.ios.kt` | Source Component | 19 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/streams/BingeGroupCacheStorage.ios.kt` | Source Component | 17 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/streams/EpochMs.ios.kt` | Source Component | 7 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/streams/StreamBadgeSettingsStorage.ios.kt` | Source Component | 97 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/streams/StreamLinkCacheStorage.ios.kt` | Source Component | 17 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/tmdb/TmdbSettingsStorage.ios.kt` | Source Component | 179 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/trakt/TraktAuthStorage.ios.kt` | Source Component | 19 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/trakt/TraktBrandPainter.ios.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/trakt/TraktCommentsStorage.ios.kt` | Source Component | 40 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/trakt/TraktLibraryStorage.ios.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/trakt/TraktPlatformClock.ios.kt` | Source Component | 14 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/trakt/TraktSettingsStorage.ios.kt` | Source Component | 34 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/updater/AppUpdaterPlatform.ios.kt` | Source Component | 36 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/watched/WatchedClock.ios.kt` | Source Component | 10 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/watched/WatchedStorage.ios.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/watchprogress/ContinueWatchingEnrichmentStorage.ios.kt` | Source Component | 16 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/watchprogress/ContinueWatchingPreferencesStorage.ios.kt` | Source Component | 15 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/watchprogress/CurrentDateProvider.ios.kt` | Source Component | 19 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/watchprogress/ResumePromptStorage.ios.kt` | Source Component | 27 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/watchprogress/WatchProgressClock.ios.kt` | Source Component | 9 | DOCUMENTED |  |
| `composeApp/src/iosMain/kotlin/com/nuvio/app/features/watchprogress/WatchProgressStorage.ios.kt` | Source Component | 14 | DOCUMENTED |  |

### Desktop Platform Implementation

| File Path | Type / Role | Lines | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `composeApp/src/desktopMain/kotlin/com/nuvio/app/core/network/ServerConfigurationStorage.desktop.kt` | Configuration | 7 | DOCUMENTED |  |
| `composeApp/src/desktopMain/kotlin/com/nuvio/app/features/details/components/BackdropPaletteImage.desktop.kt` | Source Component | 6 | DOCUMENTED |  |
| `composeApp/src/desktopMain/kotlin/com/nuvio/app/features/updater/AppUpdaterPlatform.desktop.kt` | Source Component | 36 | DOCUMENTED |  |

### Prebuilt Native AARs / Libraries

| File Path | Type / Role | Lines | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `composeApp/libs/lib-common-release.aar` | Native AAR Library | 4165 | DOCUMENTED |  |
| `composeApp/libs/lib-datasource-okhttp-release.aar` | Native AAR Library | 73 | DOCUMENTED |  |
| `composeApp/libs/lib-datasource-release.aar` | Native AAR Library | 1277 | DOCUMENTED |  |
| `composeApp/libs/lib-decoder-av1-release.aar` | Native AAR Library | 20022 | DOCUMENTED |  |
| `composeApp/libs/lib-decoder-ffmpeg-release.aar` | Native AAR Library | 63085 | DOCUMENTED |  |
| `composeApp/libs/lib-decoder-mpegh-release.aar` | Native AAR Library | 9126 | DOCUMENTED |  |
| `composeApp/libs/lib-exoplayer-hls-release.aar` | Native AAR Library | 1576 | DOCUMENTED |  |
| `composeApp/libs/lib-exoplayer-release.aar` | Native AAR Library | 12125 | DOCUMENTED |  |
| `composeApp/libs/lib-extractor-release.aar` | Native AAR Library | 5821 | DOCUMENTED |  |
| `composeApp/libs/lib-nuvio-engine-android-0.1.4.aar` | Native AAR Library | 113324 | DOCUMENTED |  |
| `composeApp/libs/lib-ui-release.aar` | Native AAR Library | 3003 | DOCUMENTED |  |
| `composeApp/libs/quickjs-kt-android-1.0.5-nuvio.aar` | Native AAR Library | 13199 | DOCUMENTED |  |
| `composeApp/libs/test.yml` | Source Component | 1 | DOCUMENTED |  |

### Android XML Resources & Manifests

| File Path | Type / Role | Lines | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `androidApp/src/debug/AndroidManifest.xml` | XML Config / Resource | 9 | DOCUMENTED |  |
| `androidApp/src/debug/res/values/strings.xml` | XML Config / Resource | 4 | DOCUMENTED |  |
| `androidApp/src/full/AndroidManifest.xml` | XML Config / Resource | 6 | DOCUMENTED |  |
| `androidApp/src/main/AndroidManifest.xml` | XML Config / Resource | 189 | DOCUMENTED |  |
| `androidApp/src/playstore/AndroidManifest.xml` | XML Config / Resource | 24 | DOCUMENTED |  |
| `composeApp/src/androidMain/AndroidManifest.xml` | XML Config / Resource | 19 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/drawable/ic_launcher_background.xml` | XML Config / Resource | 170 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/drawable/ic_notification_small.xml` | XML Config / Resource | 32 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/drawable/ic_player_aspect_ratio.xml` | XML Config / Resource | 10 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/drawable/ic_player_audio_filled.xml` | XML Config / Resource | 17 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/drawable/ic_player_episodes.xml` | XML Config / Resource | 11 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/drawable/ic_player_pause.xml` | XML Config / Resource | 13 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/drawable/ic_player_play.xml` | XML Config / Resource | 10 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/drawable/ic_player_source.xml` | XML Config / Resource | 14 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/drawable/ic_player_subtitles.xml` | XML Config / Resource | 12 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/drawable/library_add_plus.xml` | XML Config / Resource | 14 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/drawable/mdblist_logo.xml` | XML Config / Resource | 27 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/drawable/simkl_logo_glyph.xml` | XML Config / Resource | 22 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/drawable/simkl_logo_wordmark.xml` | XML Config / Resource | 13 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/drawable/trakt_logo_wordmark.xml` | XML Config / Resource | 14 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/drawable/trakt_tv_favicon.xml` | XML Config / Resource | 12 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/layout/exo_player_view.xml` | XML Config / Resource | 54 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/mipmap-anydpi-v26/ic_launcher.xml` | XML Config / Resource | 6 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/mipmap-anydpi-v26/ic_launcher_arctic_blue.xml` | XML Config / Resource | 6 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/mipmap-anydpi-v26/ic_launcher_copper.xml` | XML Config / Resource | 6 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/mipmap-anydpi-v26/ic_launcher_emerald.xml` | XML Config / Resource | 6 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/mipmap-anydpi-v26/ic_launcher_graphite.xml` | XML Config / Resource | 6 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/mipmap-anydpi-v26/ic_launcher_rose_gold.xml` | XML Config / Resource | 6 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/mipmap-anydpi-v26/ic_launcher_round.xml` | XML Config / Resource | 6 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/values-bn/strings.xml` | XML Config / Resource | 4 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/values-es/strings.xml` | XML Config / Resource | 4 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/values-v31/themes.xml` | XML Config / Resource | 32 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/values/colors.xml` | XML Config / Resource | 5 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/values/ids.xml` | XML Config / Resource | 6 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/values/strings.xml` | XML Config / Resource | 3 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/values/themes.xml` | XML Config / Resource | 13 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/xml/file_paths.xml` | XML Config / Resource | 12 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/xml/locale_config.xml` | XML Config / Resource | 27 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/xml/network_security_config.xml` | XML Config / Resource | 10 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/drawable/compose-multiplatform.xml` | XML Config / Resource | 44 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/drawable/emby_logo.xml` | XML Config / Resource | 12 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/drawable/sidebar_home.xml` | XML Config / Resource | 18 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/drawable/sidebar_library.xml` | XML Config / Resource | 17 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/drawable/sidebar_search.xml` | XML Config / Resource | 11 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/drawable/sidebar_settings.xml` | XML Config / Resource | 11 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-ar/strings.xml` | XML Config / Resource | 2163 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-bg/strings.xml` | XML Config / Resource | 2273 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-bn/strings.xml` | XML Config / Resource | 2475 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-bn/strings_mdblist.xml` | XML Config / Resource | 55 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-bn/strings_servers.xml` | XML Config / Resource | 58 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-cs/strings.xml` | XML Config / Resource | 2295 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-de/strings.xml` | XML Config / Resource | 1265 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-el/strings.xml` | XML Config / Resource | 2383 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-es/strings.xml` | XML Config / Resource | 2354 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-fr/strings.xml` | XML Config / Resource | 2002 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-he/strings.xml` | XML Config / Resource | 776 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-hr/strings.xml` | XML Config / Resource | 2414 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-hu/strings.xml` | XML Config / Resource | 2148 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-id/strings.xml` | XML Config / Resource | 1486 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-in/strings.xml` | XML Config / Resource | 1486 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-it/strings.xml` | XML Config / Resource | 1986 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-ja/strings.xml` | XML Config / Resource | 1953 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-nb/strings.xml` | XML Config / Resource | 2328 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-nl/strings.xml` | XML Config / Resource | 2300 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-pl/strings.xml` | XML Config / Resource | 2337 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-pt-rBR/strings.xml` | XML Config / Resource | 1405 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-pt/strings.xml` | XML Config / Resource | 1890 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-ro/strings.xml` | XML Config / Resource | 1948 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-ru/strings.xml` | XML Config / Resource | 2287 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-sk/strings.xml` | XML Config / Resource | 2413 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-tr/strings.xml` | XML Config / Resource | 2213 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-ur/strings.xml` | XML Config / Resource | 2374 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-ur/strings_mdblist.xml` | XML Config / Resource | 54 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-vi/strings.xml` | XML Config / Resource | 2462 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-vi/strings_mdblist.xml` | XML Config / Resource | 54 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values-vi/strings_servers.xml` | XML Config / Resource | 62 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values/strings.xml` | XML Config / Resource | 2462 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values/strings_mdblist.xml` | XML Config / Resource | 54 | DOCUMENTED |  |
| `composeApp/src/commonMain/composeResources/values/strings_servers.xml` | XML Config / Resource | 62 | DOCUMENTED |  |

### Documentation & Stremio Reference Specs

| File Path | Type / Role | Lines | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `Docs/Stremio addons refer/README.md` | Source Component | 152 | DOCUMENTED |  |
| `Docs/Stremio addons refer/advanced.md` | Source Component | 429 | DOCUMENTED |  |
| `Docs/Stremio addons refer/api/README.md` | Source Component | 41 | DOCUMENTED |  |
| `Docs/Stremio addons refer/api/requests/defineCatalogHandler.md` | Source Component | 90 | DOCUMENTED |  |
| `Docs/Stremio addons refer/api/requests/defineMetaHandler.md` | Source Component | 53 | DOCUMENTED |  |
| `Docs/Stremio addons refer/api/requests/defineResourceHandler.md` | Source Component | 57 | DOCUMENTED |  |
| `Docs/Stremio addons refer/api/requests/defineStreamHandler.md` | Source Component | 51 | DOCUMENTED |  |
| `Docs/Stremio addons refer/api/requests/defineSubtitlesHandler.md` | Source Component | 60 | DOCUMENTED |  |
| `Docs/Stremio addons refer/api/responses/addon_catalog.md` | Source Component | 9 | DOCUMENTED |  |
| `Docs/Stremio addons refer/api/responses/content.types.md` | Source Component | 10 | DOCUMENTED |  |
| `Docs/Stremio addons refer/api/responses/manifest.md` | Source Component | 180 | DOCUMENTED |  |
| `Docs/Stremio addons refer/api/responses/meta.links.md` | Source Component | 13 | DOCUMENTED |  |
| `Docs/Stremio addons refer/api/responses/meta.md` | Source Component | 141 | DOCUMENTED |  |
| `Docs/Stremio addons refer/api/responses/stream.md` | Source Component | 58 | DOCUMENTED |  |
| `Docs/Stremio addons refer/api/responses/subtitles.md` | Source Component | 15 | DOCUMENTED |  |
| `Docs/Stremio addons refer/deep-links.md` | Source Component | 71 | DOCUMENTED |  |
| `Docs/Stremio addons refer/deploying/README.md` | Source Component | 34 | DOCUMENTED |  |
| `Docs/Stremio addons refer/deploying/beamup.md` | Source Component | 38 | DOCUMENTED |  |
| `Docs/Stremio addons refer/deploying/glitch.md` | Source Component | 53 | DOCUMENTED |  |
| `Docs/Stremio addons refer/deploying/now.md` | Source Component | 65 | DOCUMENTED |  |
| `Docs/Stremio addons refer/examples.md` | Source Component | 34 | DOCUMENTED |  |
| `Docs/Stremio addons refer/protocol.md` | Source Component | 194 | DOCUMENTED |  |
| `Docs/Stremio addons refer/testing.md` | Source Component | 41 | DOCUMENTED |  |

### Other Source File

| File Path | Type / Role | Lines | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `.github/ISSUE_TEMPLATE/bug_report.yml` | Source Component | 235 | DOCUMENTED |  |
| `.github/ISSUE_TEMPLATE/config.yml` | Source Component | 8 | DOCUMENTED |  |
| `.github/ISSUE_TEMPLATE/feature_request.yml` | Source Component | 83 | DOCUMENTED |  |
| `.github/PULL_REQUEST_TEMPLATE.md` | Source Component | 67 | DOCUMENTED |  |
| `.github/workflows/android-release.yml` | Source Component | 319 | DOCUMENTED |  |
| `.github/workflows/close-stale-issues.yml` | Source Component | 103 | DOCUMENTED |  |
| `.github/workflows/close-unlabeled-issues.yml` | Source Component | 98 | DOCUMENTED |  |
| `.github/workflows/discontinue-legacy-main.yml` | Source Component | 199 | DOCUMENTED |  |
| `.github/workflows/ios-test-build.yml` | Source Component | 112 | DOCUMENTED |  |
| `.github/workflows/pr-full-debug-build-comment.yml` | Source Component | 112 | DOCUMENTED |  |
| `.github/workflows/pr-full-debug-build.yml` | Source Component | 54 | DOCUMENTED |  |
| `.github/workflows/pr-template-check.yml` | Source Component | 148 | DOCUMENTED |  |
| `.github/workflows/stale-needs-info.yml` | Source Component | 81 | DOCUMENTED |  |
| `.github/workflows/triage-needs-info.yml` | Source Component | 257 | DOCUMENTED |  |
| `.github/workflows/update-store-source.yml` | Source Component | 111 | DOCUMENTED |  |
| `CONTRIBUTING.md` | Source Component | 181 | DOCUMENTED |  |
| `LICENSE` | Source Component | 674 | DOCUMENTED |  |
| `README.md` | Source Component | 55 | DOCUMENTED |  |
| `androidApp/src/debug/kotlin/com/nuvio/android/BackgroundDownloadTestActivity.kt` | Source Component | 41 | DOCUMENTED |  |
| `androidApp/src/main/baseline-prof.txt` | Source Component | 11375 | DOCUMENTED |  |
| `assets/animations/loading/reference.json` | JSON Specification / Data | 1 | DOCUMENTED |  |
| `composeApp/proguard-rules.pro` | Source Component | 64 | DOCUMENTED |  |
| `composeApp/src/androidMain/assets/licenses/react-native-jelly-tabs.txt` | Source Component | 25 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/raw/isrg_root_x1.pem` | Source Component | 31 | DOCUMENTED |  |
| `composeApp/src/androidMain/res/raw/isrg_root_x2.pem` | Source Component | 14 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/App.kt` | Source Component | 105 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/AppGate.kt` | Source Component | 574 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/AppGateController.kt` | Source Component | 30 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/AppGateOverlay.kt` | Source Component | 45 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/AppNavigationSupport.kt` | Source Component | 105 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/AppScreenTab.kt` | Screen UI (Compose) | 30 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/AppShellComponents.kt` | Source Component | 245 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/CatalogDestination.kt` | Source Component | 86 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/DetailsDestinations.kt` | Source Component | 166 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/DownloadsDestinations.kt` | Source Component | 42 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/Greeting.kt` | Source Component | 9 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/MainAppContent.kt` | Source Component | 2024 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/MainTabsDestination.kt` | Source Component | 211 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/Platform.kt` | Source Component | 11 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/PlayerDestination.kt` | Player Component | 129 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/RootTabHost.kt` | Source Component | 106 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/SettingsDestinations.kt` | Source Component | 158 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/StreamDestination.kt` | Source Component | 808 | DOCUMENTED |  |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/StreamLoadingScreen.kt` | Screen UI (Compose) | 45 | DOCUMENTED |  |
| `composeApp/src/nativeInterop/cinterop/AppIconBridge.h` | Source Component | 9 | DOCUMENTED |  |
| `composeApp/src/nativeInterop/cinterop/appicon.def` | Source Component | 2 | DOCUMENTED |  |
| `composeApp/src/nativeInterop/cinterop/commoncrypto.def` | Source Component | 3 | DOCUMENTED |  |
| `composeApp/src/nativeInterop/cinterop/commoncrypto_shim.h` | Source Component | 190 | DOCUMENTED |  |
| `composeApp/src/nativeInterop/cinterop/nuvioengine.def` | Source Component | 3 | DOCUMENTED |  |
| `gradle/wrapper/gradle-wrapper.properties` | Source Component | 7 | DOCUMENTED |  |
| `iosApp/DownloadsWidgetExtension/DownloadsLiveActivityWidget.swift` | Source Component | 203 | DOCUMENTED |  |
| `iosApp/DownloadsWidgetExtension/DownloadsWidgetBundle.swift` | Source Component | 9 | DOCUMENTED |  |
| `iosApp/DownloadsWidgetExtension/Info.plist` | Source Component | 31 | DOCUMENTED |  |
| `iosApp/iosApp/AppIconBridge.m` | Source Component | 41 | DOCUMENTED |  |
| `iosApp/iosApp/Assets.xcassets/AccentColor.colorset/Contents.json` | JSON Specification / Data | 11 | DOCUMENTED |  |
| `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/Contents.json` | JSON Specification / Data | 36 | DOCUMENTED |  |
| `iosApp/iosApp/Assets.xcassets/AppIconArcticBlue.appiconset/Contents.json` | JSON Specification / Data | 36 | DOCUMENTED |  |
| `iosApp/iosApp/Assets.xcassets/AppIconCopper.appiconset/Contents.json` | JSON Specification / Data | 36 | DOCUMENTED |  |
| `iosApp/iosApp/Assets.xcassets/AppIconEmerald.appiconset/Contents.json` | JSON Specification / Data | 36 | DOCUMENTED |  |
| `iosApp/iosApp/Assets.xcassets/AppIconGraphite.appiconset/Contents.json` | JSON Specification / Data | 36 | DOCUMENTED |  |
| `iosApp/iosApp/Assets.xcassets/AppIconRoseGold.appiconset/Contents.json` | JSON Specification / Data | 36 | DOCUMENTED |  |
| `iosApp/iosApp/Assets.xcassets/Contents.json` | JSON Specification / Data | 6 | DOCUMENTED |  |
| `iosApp/iosApp/Assets.xcassets/NuvioTabHome.imageset/Contents.json` | JSON Specification / Data | 16 | DOCUMENTED |  |
| `iosApp/iosApp/Assets.xcassets/NuvioTabLibrary.imageset/Contents.json` | JSON Specification / Data | 16 | DOCUMENTED |  |
| `iosApp/iosApp/Assets.xcassets/NuvioTabProfile.imageset/Contents.json` | JSON Specification / Data | 16 | DOCUMENTED |  |
| `iosApp/iosApp/Assets.xcassets/NuvioTabSearch.imageset/Contents.json` | JSON Specification / Data | 16 | DOCUMENTED |  |
| `iosApp/iosApp/ContentView.swift` | Source Component | 1411 | DOCUMENTED |  |
| `iosApp/iosApp/DownloadsLiveActivityManager.swift` | Source Component | 129 | DOCUMENTED |  |
| `iosApp/iosApp/Info.plist` | Source Component | 75 | DOCUMENTED |  |
| `iosApp/iosApp/OrientationLockCoordinator.swift` | Source Component | 133 | DOCUMENTED |  |
| `iosApp/iosApp/Player/MPVPlayerBridge.swift` | Player Component | 1369 | DOCUMENTED |  |
| `iosApp/iosApp/Player/MetalLayer.swift` | Player Component | 28 | DOCUMENTED |  |
| `iosApp/iosApp/Player/NowPlayingController.swift` | Player Component | 274 | DOCUMENTED |  |
| `iosApp/iosApp/Preview Content/Preview Assets.xcassets/Contents.json` | JSON Specification / Data | 6 | DOCUMENTED |  |
| `iosApp/iosApp/Resources/Fonts/OFL.txt` | Source Component | 92 | DOCUMENTED |  |
| `iosApp/iosApp/SystemUI.swift` | Source Component | 49 | DOCUMENTED |  |
| `iosApp/iosApp/iOSApp.swift` | Source Component | 17 | DOCUMENTED |  |
| `scripts/build-ios-ipa.sh` | Source Component | 116 | DOCUMENTED |  |
| `scripts/debug_logs.sh` | Source Component | 372 | DOCUMENTED |  |
| `scripts/generate-release-notes.sh` | Source Component | 174 | DOCUMENTED |  |
| `scripts/generate_app_icon_assets.py` | Source Component | 370 | DOCUMENTED |  |
| `scripts/generate_logo_colorways.py` | Source Component | 500 | DOCUMENTED |  |
| `scripts/prepare-ios-dependencies.sh` | Source Component | 47 | DOCUMENTED |  |
| `scripts/release-metadata.sh` | Source Component | 54 | DOCUMENTED |  |
| `scripts/run-mobile.sh` | Source Component | 476 | DOCUMENTED |  |
| `scripts/test-mpv-audio-preferences.py` | Source Component | 90 | DOCUMENTED |  |
| `scripts/update-store-source.py` | Source Component | 184 | DOCUMENTED |  |
| `store.json` | JSON Specification / Data | 292 | DOCUMENTED |  |

## Skipped Files & Rationale

| Path | Reason Skipped |
| :--- | :--- |
| `.gitignore` | Version control or Gradle wrapper script |
| `.gitmodules` | Version control or Gradle wrapper script |
| `androidApp/src/androidTest/kotlin/com/nuvio/android/BackgroundDownloadTest.kt` | Automated test suite (unit/host/instrumentation) |
| `androidApp/src/androidTest/kotlin/com/nuvio/android/LibraryTrackingUiTest.kt` | Automated test suite (unit/host/instrumentation) |
| `androidApp/src/androidTest/kotlin/com/nuvio/android/MdbListLiveTest.kt` | Automated test suite (unit/host/instrumentation) |
| `androidApp/src/androidTest/kotlin/com/nuvio/android/MdbListSettingsUiTest.kt` | Automated test suite (unit/host/instrumentation) |
| `assets/ratings/icons/rating_audience_score.svg` | Static media / font binary asset |
| `assets/ratings/icons/rating_audience_stale.svg` | Static media / font binary asset |
| `assets/ratings/icons/rating_audience_verified_hot.svg` | Static media / font binary asset |
| `assets/ratings/icons/rating_rotten_tomatoes.svg` | Static media / font binary asset |
| `assets/ratings/icons/rating_rotten_tomatoes_certified.svg` | Static media / font binary asset |
| `assets/ratings/icons/rating_rotten_tomatoes_rotten.svg` | Static media / font binary asset |
| `composeApp/src/androidFullHostTest/kotlin/com/nuvio/app/features/plugins/PluginScraperCodeFileStoreTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidFullHostTest/kotlin/com/nuvio/app/features/plugins/PluginSyncTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidFullHostTest/kotlin/com/nuvio/app/features/plugins/runtime/BinaryFetchTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidFullHostTest/kotlin/com/nuvio/app/features/trailer/InAppYouTubeExtractorSingleUrlTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidFullHostTest/kotlin/com/nuvio/app/features/updater/AppUpdaterControllerTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidFullHostTest/kotlin/com/nuvio/app/features/updater/UpdatePreferencesTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/RootTabHostTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/StreamOrientationTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/core/format/ReleaseDateDisplayTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/core/storage/LocalAccountDataCleanerTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/core/ui/FloatingNavigationBarGestureTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/core/ui/FloatingNavigationBarRtlTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/core/ui/LoadingIndicatorTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/core/ui/PosterLiftSourceTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/core/ui/PosterOpenMotionTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/core/ui/ScreenActivityEffectTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/core/ui/ShimmerTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/core/ui/jelly/JellyMotionTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/details/MetaDetailsPosterNavigationTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/details/RatingsSettingsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/details/RatingsVisibilityTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/downloads/AndroidDownloadLifecycleTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/downloads/AndroidDownloadTransferTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/downloads/DownloadSubtitlesTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/downloads/DownloadsTransferWorkerTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/home/CachedNextUpReleaseTimeZoneTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/home/HomeCatalogSettingsRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/home/HomeHeroPagerTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/mdblist/MdbListSettingsRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/p2p/P2pStreamingEngineAndroidTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/player/ExternalPlaybackProgressTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/player/NextEpisodeCardTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/player/OpeningOverlayTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/player/PauseOverlaySettingsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/player/PlaybackSubtitleMimeTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/player/PlayerAutoPlayTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/player/PlayerBrightnessTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/player/PlayerGestureOverlayTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/player/PlayerLibassCompatTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/player/PlayerLoadingSettingsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/player/PlayerNextEpisodeTransitionTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/player/PlayerNowPlayingServiceTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/player/PlayerSubtitleRestoreTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/player/PlayerSurfaceGesturesTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/profiles/AvatarPickerTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/search/SearchHistoryPreferencesTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/settings/ThemeSettingsRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/settings/ThemeSettingsStorageTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/shuffle/EpisodeShuffleSheetTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/shuffle/EpisodeShuffleStorageTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/streams/ProviderFilterRowTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/streams/StreamBackgroundSettingsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/tmdb/TmdbSettingsRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/trailer/TrailerPlayerTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/features/watchprogress/ContinueWatchingEnrichmentStorageTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidHostTest/kotlin/com/nuvio/app/navigation/PosterNavigationTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/androidMain/res/drawable-nodpi/ic_splash_logo.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/drawable-nodpi/ic_splash_logo_arctic_blue.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/drawable-nodpi/ic_splash_logo_copper.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/drawable-nodpi/ic_splash_logo_emerald.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/drawable-nodpi/ic_splash_logo_graphite.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/drawable-nodpi/ic_splash_logo_rose_gold.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-hdpi/ic_launcher.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-hdpi/ic_launcher_arctic_blue.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-hdpi/ic_launcher_arctic_blue_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-hdpi/ic_launcher_copper.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-hdpi/ic_launcher_copper_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-hdpi/ic_launcher_emerald.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-hdpi/ic_launcher_emerald_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-hdpi/ic_launcher_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-hdpi/ic_launcher_graphite.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-hdpi/ic_launcher_graphite_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-hdpi/ic_launcher_monochrome.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-hdpi/ic_launcher_rose_gold.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-hdpi/ic_launcher_rose_gold_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-hdpi/ic_launcher_round.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-mdpi/ic_launcher.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-mdpi/ic_launcher_arctic_blue.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-mdpi/ic_launcher_arctic_blue_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-mdpi/ic_launcher_copper.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-mdpi/ic_launcher_copper_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-mdpi/ic_launcher_emerald.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-mdpi/ic_launcher_emerald_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-mdpi/ic_launcher_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-mdpi/ic_launcher_graphite.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-mdpi/ic_launcher_graphite_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-mdpi/ic_launcher_monochrome.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-mdpi/ic_launcher_rose_gold.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-mdpi/ic_launcher_rose_gold_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-mdpi/ic_launcher_round.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xhdpi/ic_launcher.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xhdpi/ic_launcher_arctic_blue.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xhdpi/ic_launcher_arctic_blue_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xhdpi/ic_launcher_copper.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xhdpi/ic_launcher_copper_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xhdpi/ic_launcher_emerald.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xhdpi/ic_launcher_emerald_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xhdpi/ic_launcher_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xhdpi/ic_launcher_graphite.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xhdpi/ic_launcher_graphite_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xhdpi/ic_launcher_monochrome.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xhdpi/ic_launcher_rose_gold.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xhdpi/ic_launcher_rose_gold_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xhdpi/ic_launcher_round.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxhdpi/ic_launcher.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxhdpi/ic_launcher_arctic_blue.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxhdpi/ic_launcher_arctic_blue_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxhdpi/ic_launcher_copper.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxhdpi/ic_launcher_copper_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxhdpi/ic_launcher_emerald.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxhdpi/ic_launcher_emerald_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxhdpi/ic_launcher_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxhdpi/ic_launcher_graphite.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxhdpi/ic_launcher_graphite_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxhdpi/ic_launcher_monochrome.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxhdpi/ic_launcher_rose_gold.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxhdpi/ic_launcher_rose_gold_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxhdpi/ic_launcher_round.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher_arctic_blue.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher_arctic_blue_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher_copper.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher_copper_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher_emerald.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher_emerald_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher_graphite.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher_graphite_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher_monochrome.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher_rose_gold.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher_rose_gold_foreground.webp` | Static media / font binary asset |
| `composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher_round.webp` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/app_icon_arctic_blue.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/app_icon_copper.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/app_icon_emerald.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/app_icon_graphite.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/app_icon_original.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/app_icon_rose_gold.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/app_logo_wordmark.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/app_logo_wordmark_arctic_blue.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/app_logo_wordmark_copper.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/app_logo_wordmark_emerald.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/app_logo_wordmark_gold.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/app_logo_wordmark_graphite.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/app_logo_wordmark_original.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/app_logo_wordmark_rose_gold.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/ic_player_aspect_ratio.svg` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/ic_player_audio_filled.svg` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/ic_player_episodes.svg` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/ic_player_pause.svg` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/ic_player_play.svg` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/ic_player_source.svg` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/ic_player_subtitles.svg` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/introdb_favicon.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/jellyfin_logo.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/library_add_plus.svg` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/mdblist_logo.svg` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/rating_audience_score.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/rating_audience_stale.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/rating_audience_verified_hot.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/rating_imdb.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/rating_letterboxd.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/rating_mal.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/rating_metacritic.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/rating_rotten_tomatoes.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/rating_rotten_tomatoes_certified.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/rating_rotten_tomatoes_rotten.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/rating_tmdb.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/rating_trakt.png` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/simkl_logo_glyph.svg` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/simkl_logo_wordmark.svg` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/trailer_play_button.svg` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/trakt_logo_wordmark.svg` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/drawable/trakt_tv_favicon.svg` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/font/jetbrains_sans_bold.ttf` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/font/jetbrains_sans_regular.ttf` | Static media / font binary asset |
| `composeApp/src/commonMain/composeResources/font/jetbrains_sans_semibold.ttf` | Static media / font binary asset |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/ComposeAppCommonTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/core/auth/DeviceSessionRegistrationTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/core/concurrent/ConflatedTaskDispatcherTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/core/deeplink/AppUrlBridgeTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/core/network/BackendRateLimitTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/core/poster/CustomPosterOverlayTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/core/poster/CustomPosterUrlResolverTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/core/sync/ProfileSettingsCredentialPolicyTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/core/sync/ProviderCredentialModelsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/core/sync/SyncManagerTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/core/time/EpisodeReleaseDateParserTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/core/ui/CustomThemeColorsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/core/ui/DuplicateSafeLazyKeysTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/core/ui/ScopedDisintegrationTrackerTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/addons/AddonModelsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/catalog/CatalogPaginationStateTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/catalog/LibraryCatalogStateTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/cloud/CloudLibraryStoreTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/cloud/PremiumizeCloudLibraryProviderApiTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/cloud/TorboxCloudLibraryFixtures.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/cloud/TorboxCloudLibraryProviderApiTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/collection/CollectionRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/collection/CollectionSourceSerializationTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/debrid/DebridFileSelectorReleaseTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/debrid/DebridFileSelectorTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/debrid/DebridMagnetBuilderTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/debrid/DebridProviderTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/debrid/DebridSettingsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/debrid/DebridStreamPresentationTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/debrid/DirectDebridStreamPreparerTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/debrid/PremiumizeDeviceAuthTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/debrid/TorboxDeviceAuthTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/details/HeroTrailerSelectorTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/details/MetaDetailsParserTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/details/MetaDetailsReleaseLineTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/details/MetaScreenSectionLayoutTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/details/SeasonPosterParsingTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/details/SeriesPlaybackResolverTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/details/SeriesSeasonSupportTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/home/CachedNextUpReleaseTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/home/HomeCatalogDefinitionTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/home/HomeCatalogParserTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/home/HomeCatalogSectionTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/home/HomeCatalogSettingsSyncServiceTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/home/HomeCollectionDefinitionsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/home/HomeScreenTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/home/ReleaseInfoUtilsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/home/components/HomeContinueWatchingArtworkTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/home/components/HomeHeroSectionTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/library/LibraryDeltaStateTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/library/LibraryDisplaySettingsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/library/LibraryListManagementControllerTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/library/LibraryProviderOrdersTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/library/LibraryRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/library/LibraryServerContentTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/library/LibrarySyncReconcilerTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/library/sync/LibrarySyncPagingTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/library/sync/SupabaseLibrarySyncAdapterTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListAccountControllerTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListApiClientTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListAuthRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListAuthStoreTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListHistoryServiceTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListHttpClientTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListLibraryDecoderTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListLibraryServiceTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListLibrarySorterTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListLibraryTestFixtures.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListLibraryWriterTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListMobileParityTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListMutationTargetTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListNetworkEngineTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListProgressProjectionTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListRatingsClientTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListRatingsDecoderTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListRatingsRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListScrobbleServiceTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListSyncDecoderTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListSyncEngineTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListSyncRemoteTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListSyncRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListSyncTestFixtures.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListTestFixtures.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/MdbListTrackingAdaptersTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/mdblist/RottenTomatoesRatingTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/membership/ProfileBackgroundTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/membership/ThemeAccessTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/p2p/P2pMagnetTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/p2p/P2pTelemetryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/player/ClickableIncludingFlingStopTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/player/InfusePlaybackTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/player/OriginalAudioPreferenceTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/player/ParentalGuideRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/player/PlayerAudioRestoreTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/player/PlayerLaunchStoreTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/player/PlayerLoadingStatusTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/player/PlayerScreenRuntimeStateTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/player/PlayerServerAudioTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/player/PlayerStartupSubtitlesTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/player/PlayerSubtitleUtilsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/player/PlayerTrackSelectionTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/player/SubtitleSelectionModelTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/player/VodCacheSizingTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/plugins/PluginContentIdsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/plugins/PluginPersistenceTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/profiles/AvatarAccessTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/profiles/ProfileSelectionRoutingTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/search/SearchHistoryRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/search/SearchRequestStateTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/servers/FakeServerProvider.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/servers/ServerCatalogTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/servers/ServerItemRefTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/servers/ServerMatcherTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/servers/ServerPlaybackTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/servers/ServerSyncTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/servers/ServerUserStateProjectionTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/servers/ServerWatchedTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/servers/mediabrowser/MediaBrowserConnectionTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/servers/mediabrowser/MediaBrowserMapperTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/servers/mediabrowser/MediaBrowserPlaybackTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/servers/mediabrowser/MediaBrowserRequestTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/servers/mediabrowser/TestHttp.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/settings/AppIconOptionTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/settings/MemberBrandWordmarkTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/settings/TrackingSettingsPresentationTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/shuffle/EpisodeShuffleTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/shuffle/RandomEpisodePickerTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/shuffle/ShuffleProjectionTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/simkl/SimklAdapterProjectionCacheTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/simkl/SimklAnimeWatchedResolutionTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/simkl/SimklApiClientTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/simkl/SimklHiddenContentIndexTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/simkl/SimklHistoryPushGuardTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/simkl/SimklMutationReconciliationTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/simkl/SimklMutationRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/simkl/SimklPkceTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/simkl/SimklPlaybackReconciliationTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/simkl/SimklProjectionsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/simkl/SimklRefreshPolicyTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/simkl/SimklScrobbleReconciliationTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/simkl/SimklSettingsPolicyTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/simkl/SimklSyncEngineTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/streams/PlaybackAvailabilityTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/streams/PlaybackUrlCredentialsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/streams/StreamAutoPlayLoadingPolicyTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/streams/StreamAutoPlaySelectorTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/streams/StreamBadgePresentationTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/streams/StreamLinkCacheRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/streams/StreamModelsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/streams/StreamParserTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/streams/StreamResumeStateTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/streams/StreamYouTubeTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/streams/YouTubeStreamResolverTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/tmdb/TmdbAgeRatingsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/tmdb/TmdbImagesTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/tmdb/TmdbMetadataServiceTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/tracking/TrackingAttributionTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/tracking/TrackingMediaTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/tracking/TrackingReadsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/tracking/TrackingScrobbleCoordinatorTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/tracking/TrackingSourcesTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/trailer/LetterboxDetectorTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/trakt/TraktAuthRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/trakt/TraktEpisodeMappingServiceTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/trakt/TraktIdUtilsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/trakt/TraktImageUtilsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/trakt/TraktProgressRemovalTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/trakt/TraktPublicListSourceResolverTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/trakt/TraktScrobbleRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/trakt/TraktSettingsRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/trakt/TraktTrackingLibraryProviderTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/trakt/TraktTrackingProgressProviderTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/trakt/TraktWatchedShowSnapshotRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/updater/AppUpdaterRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/updater/ReleaseSelectorTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/updater/UpdateChannelTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/updater/VersionUtilsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/watched/StoredWatchedPayloadTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/watched/WatchedItemsStoreTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/watched/WatchedModelsTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/watched/WatchedRepositoryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/watching/application/WatchingStateTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/watching/domain/SeriesContinuityTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/watching/domain/WatchingPoliciesTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/watching/sync/TraktWatchedSyncAdapterTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/watchprogress/ContinueWatchingEnrichmentCacheTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/watchprogress/RemoteProgressWriteDeduplicatorTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/watchprogress/TrackingProviderRefreshCoordinatorTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/watchprogress/WatchProgressIdentityTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/watchprogress/WatchProgressMetadataProjectionTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/watchprogress/WatchProgressRulesTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/watchprogress/WatchProgressSourceCoordinatorTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/features/watchprogress/WatchProgressSourceProjectionTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/commonTest/kotlin/com/nuvio/app/navigation/RouteDisposalRegistryTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/iosTest/kotlin/com/nuvio/app/features/mdblist/MdbListPlatformPersistenceTest.kt` | Automated test suite (unit/host/instrumentation) |
| `composeApp/src/iosTest/resources/keychain-test.entitlements` | Automated test suite (unit/host/instrumentation) |
| `gradle/wrapper/gradle-wrapper.jar` | Standalone binary archive (gradle wrapper jar) |
| `gradlew` | Version control or Gradle wrapper script |
| `gradlew.bat` | Version control or Gradle wrapper script |
| `iosApp/iosApp.xcodeproj/project.pbxproj` | Xcode project metadata / schema |
| `iosApp/iosApp.xcodeproj/project.xcworkspace/contents.xcworkspacedata` | Xcode project metadata / schema |
| `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme` | Xcode project metadata / schema |
| `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png` | Static media / font binary asset |
| `iosApp/iosApp/Assets.xcassets/AppIconArcticBlue.appiconset/app-icon-1024.png` | Static media / font binary asset |
| `iosApp/iosApp/Assets.xcassets/AppIconCopper.appiconset/app-icon-1024.png` | Static media / font binary asset |
| `iosApp/iosApp/Assets.xcassets/AppIconEmerald.appiconset/app-icon-1024.png` | Static media / font binary asset |
| `iosApp/iosApp/Assets.xcassets/AppIconGraphite.appiconset/app-icon-1024.png` | Static media / font binary asset |
| `iosApp/iosApp/Assets.xcassets/AppIconRoseGold.appiconset/app-icon-1024.png` | Static media / font binary asset |
| `iosApp/iosApp/Assets.xcassets/NuvioTabHome.imageset/nuvio-tab-home.svg` | Static media / font binary asset |
| `iosApp/iosApp/Assets.xcassets/NuvioTabLibrary.imageset/nuvio-tab-library.svg` | Static media / font binary asset |
| `iosApp/iosApp/Assets.xcassets/NuvioTabProfile.imageset/nuvio-tab-profile.svg` | Static media / font binary asset |
| `iosApp/iosApp/Assets.xcassets/NuvioTabSearch.imageset/nuvio-tab-search.svg` | Static media / font binary asset |
| `iosApp/iosApp/Resources/Fonts/NotoSansCJKsc-Regular.otf` | Static media / font binary asset |
