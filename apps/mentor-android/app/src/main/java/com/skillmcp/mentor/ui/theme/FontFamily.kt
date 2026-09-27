package com.skillmcp.mentor.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.skillmcp.mentor.R

/**
 * Lumina's brand face: Plus Jakarta Sans (SIL OFL 1.1), bundled as one variable TTF.
 * Each weight pins the `wght` axis so Medium/SemiBold/Bold really render heavier.
 */
@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: FontWeight) =
    Font(
        resId = R.font.plus_jakarta_sans_variable,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

private val PlusJakartaSans =
    FontFamily(
        jakarta(FontWeight.Normal),
        jakarta(FontWeight.Medium),
        jakarta(FontWeight.SemiBold),
        jakarta(FontWeight.Bold),
        jakarta(FontWeight.ExtraBold),
    )

@Composable
fun mentorFontFamily(): FontFamily = PlusJakartaSans
