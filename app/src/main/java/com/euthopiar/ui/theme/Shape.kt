package com.euthopiar.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ============================================================================
// Shape Tokens (Exact 4-tier system)
// ============================================================================
val ShapeS = RoundedCornerShape(8.dp)      // Posters, thumbnails, chip inners
val ShapeM = RoundedCornerShape(12.dp)     // Cards, dialogs, inputs
val ShapeL = RoundedCornerShape(20.dp)     // Bottom sheets, modal sheets
val ShapeFull = CircleShape                // Pills, circular buttons
val ShapeCurvedEdgeStart = RoundedCornerShape(topStart = 110.dp, bottomStart = 110.dp)
val ShapeCurvedEdgeEnd = RoundedCornerShape(topEnd = 110.dp, bottomEnd = 110.dp)

val AppShapes = Shapes(
    small = ShapeS,
    medium = ShapeM,
    large = ShapeL
)
