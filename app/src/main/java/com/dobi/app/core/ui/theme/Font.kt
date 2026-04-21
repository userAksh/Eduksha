package com.dobi.app.core.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.dobi.app.R

val Gilroy = FontFamily(
    Font(R.font.gilroy_bold, FontWeight.Bold, FontStyle.Normal)
)
val GilroyBold = FontWeight.Bold

val Sora = FontFamily(
    Font(R.font.sory_regular, FontWeight.Normal, FontStyle.Normal)
)
val SoraRegular = FontWeight.Normal

val SFPro = FontFamily(
    Font(R.font.sf_pro_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.sf_pro_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.sf_pro_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.sf_pro_semibold, FontWeight(590), FontStyle.Normal),
    Font(R.font.sf_pro_thin_italic, FontWeight.Thin, FontStyle.Italic),
    Font(R.font.sf_pro_light_italic, FontWeight.Light, FontStyle.Italic),
    Font(R.font.sf_pro_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.sf_pro_heavy_italic, FontWeight.ExtraBold, FontStyle.Italic),
    Font(R.font.sf_pro_black_italic, FontWeight.Black, FontStyle.Italic),
    Font(R.font.sf_pro_ultralight_italic, FontWeight.ExtraLight, FontStyle.Italic),
)

val SFProRegular = FontWeight(400)
val SFProMedium = FontWeight(510)
val SFProSemiBold = FontWeight(590)
val SFProBold = FontWeight(700)
val SFProLight = FontWeight(274)
