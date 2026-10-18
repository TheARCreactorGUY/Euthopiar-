package com.euthopiar.ui.navigation

import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * CompositionLocal providing an event flow when an already active navigation tab is re-selected.
 * Receivers can animate scroll-to-top or request focus.
 */
val LocalTabReselection = compositionLocalOf<Flow<NavTab>> { emptyFlow() }
