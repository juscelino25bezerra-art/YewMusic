package com.newtube.mobile.music

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.liskovsoft.smartyoutubetv2.tv.R

/** YEWMUSIC(compose): as cores e fontes da Proposta 2 (prancheta "Sistema visual"). */
object YewColors {
    val Grafite = Color(0xFF0F0F11)
    val Surface1 = Color(0xFF1A1A1E)
    val Surface2 = Color(0xFF26262B)
    val Text1 = Color(0xFFF4F4F5)
    val Text2 = Color(0xFFB4B4BC)
    val Text3 = Color(0xFF9A9AA3)
    val Marfim = Color(0xFFF4EFE6)
    val Rubi = Color(0xFFF2555A)
    val Glass = Color(0x1AFFFFFF)
    val GlassLine = Color(0x24FFFFFF)
}

object YewFonts {
    val Display = FontFamily(
        Font(R.font.bricolage_semibold, FontWeight.SemiBold),
        Font(R.font.bricolage_bold, FontWeight.Bold),
        Font(R.font.bricolage_extrabold, FontWeight.ExtraBold),
    )
    val Body = FontFamily(
        Font(R.font.instrument_regular, FontWeight.Normal),
        Font(R.font.instrument_medium, FontWeight.Medium),
        Font(R.font.instrument_semibold, FontWeight.SemiBold),
        Font(R.font.instrument_bold, FontWeight.Bold),
    )
}
