package com.dobi.app.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Dobi Type Scale ───────────────────────────────────────────────────────────
// Display  28/34/800  - hero headlines, primary color
// H1       22/28/700  - screen titles
// H2       18/24/700  - section titles
// H3       16/22/600  - card titles
// Body     15/22/500  - inputs, primary body
// Body Sm  14/20/400  - descriptions, sub-copy
// Caption  13/18/500  - labels
// Meta     12/16/500  - step labels, meta
// Button   16/24/600  - CTA button text

val DobiTypography = Typography(
    // Display — used for hero headings
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    // H1 — screen titles
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    // H2 — section titles
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    // H3 — card titles
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    // Body — inputs, primary body
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    // Body Sm — descriptions, sub-copy
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Caption — labels
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    // Button — CTA button text
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    // Meta — step labels, badges
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp
    ),
)
