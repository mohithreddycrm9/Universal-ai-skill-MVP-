package com.skillmcp.mentor.ui.theme

import androidx.compose.ui.graphics.Color

object BrandColors {
    val Indigo = Color(0xFF4F46E5)
    /** Dark-mode primary: lighter indigo so dark ink on it clears 4.5:1 (white on #6366F1 was 4.47:1). */
    val IndigoDark = Color(0xFF818CF8)
    val IndigoInk = Color(0xFF1E1B4B)
    val Coral = Color(0xFFFB7185)
    /** Text/icons on coral fills (white on coral is only ~2.6:1). */
    val CoralInk = Color(0xFF4C0519)
    /** Deeper coral for gradients that carry white text (white on it is ~4.7:1). */
    val CoralDeep = Color(0xFFE11D48)
    val Charcoal = Color(0xFF0F1115)
    val CharcoalCard = Color(0xFF171A21)
    val LightSurface = Color(0xFFFAFAFB)
    val LightCard = Color(0xFFFFFFFF)
}
