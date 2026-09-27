package com.skillmcp.mentor.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import com.skillmcp.mentor.R

/**
 * One calm accent per workflow category. Light tones are dark enough for icons on a 14% tint
 * (>= 3:1 non-text contrast); dark tones are lifted for charcoal surfaces.
 */
@Immutable
data class CategoryStyle(
    val icon: ImageVector,
    val light: Color,
    val dark: Color,
    @StringRes val label: Int?,
)

object CategoryPalette {
    private val fallback = CategoryStyle(Icons.Rounded.AutoAwesome, BrandColors.Indigo, BrandColors.IndigoDark, null)

    fun styleFor(category: String): CategoryStyle =
        when (category) {
            "Write" -> CategoryStyle(Icons.Rounded.EditNote, Color(0xFF4F46E5), Color(0xFFA5B4FC), R.string.category_write)
            "Learn" -> CategoryStyle(Icons.Rounded.School, Color(0xFF0F766E), Color(0xFF5EEAD4), R.string.category_learn)
            "Life" -> CategoryStyle(Icons.Rounded.Spa, Color(0xFFBE123C), Color(0xFFFDA4AF), R.string.category_life)
            "Shop" -> CategoryStyle(Icons.Rounded.ShoppingBag, Color(0xFFB45309), Color(0xFFFCD34D), R.string.category_shop)
            "Work" -> CategoryStyle(Icons.Rounded.WorkOutline, Color(0xFF6D28D9), Color(0xFFC4B5FD), R.string.category_work)
            "Privacy" -> CategoryStyle(Icons.Rounded.Lock, Color(0xFF334155), Color(0xFFCBD5E1), R.string.category_privacy)
            else -> fallback
        }
}

/** Accent for the current surface (reads the resolved scheme, so it follows the in-app theme toggle). */
@Composable
fun CategoryStyle.accent(): Color = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) dark else light
