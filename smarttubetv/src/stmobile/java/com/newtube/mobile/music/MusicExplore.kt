package com.newtube.mobile.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liskovsoft.smartyoutubetv2.tv.R
import java.util.Calendar

/**
 * YEWMUSIC(explore): o Explorar no layout Mosaico (o mesmo que o Sonivell escolheu) - gênero da
 * semana em destaque, gêneros em blocos de tamanhos diferentes, décadas em linha do tempo e
 * climas à parte. Cada bloco abre uma busca pronta, então funciona sem login e sem depender de
 * uma prateleira do YouTube.
 */
private class Genre(val name: Int, val query: Int, val color: Color)

/** Tons escuros e dessaturados: texto claro passa no contraste AA em todos. */
private val Genres = listOf(
    Genre(R.string.yew_genre_sertanejo, R.string.yew_genre_sertanejo_q, Color(0xFF5C3B1E)),
    Genre(R.string.yew_genre_funk, R.string.yew_genre_funk_q, Color(0xFF4A2030)),
    Genre(R.string.yew_genre_pagode, R.string.yew_genre_pagode_q, Color(0xFF2E4A2A)),
    Genre(R.string.yew_genre_pop, R.string.yew_genre_pop_q, Color(0xFF3D3566)),
    Genre(R.string.yew_genre_rock, R.string.yew_genre_rock_q, Color(0xFF3A2A2A)),
    Genre(R.string.yew_genre_mpb, R.string.yew_genre_mpb_q, Color(0xFF1F4A52)),
    Genre(R.string.yew_genre_gospel, R.string.yew_genre_gospel_q, Color(0xFF4A4220)),
    Genre(R.string.yew_genre_forro, R.string.yew_genre_forro_q, Color(0xFF5A2E22)),
    Genre(R.string.yew_genre_rap, R.string.yew_genre_rap_q, Color(0xFF2C3446)),
    Genre(R.string.yew_genre_eletronica, R.string.yew_genre_eletronica_q, Color(0xFF2A2F4F)),
)

private val Decades = listOf(1970, 1980, 1990, 2000, 2010, 2020)

private val Moods = listOf(
    R.string.yew_mood_relax to R.string.yew_mood_relax_query,
    R.string.yew_mood_energy to R.string.yew_mood_energy_query,
    R.string.yew_mood_focus to R.string.yew_mood_focus_query,
    R.string.yew_mood_workout to R.string.yew_mood_workout_query,
    R.string.yew_mood_sleep to R.string.yew_mood_sleep_query,
    R.string.yew_mood_party to R.string.yew_mood_party_query,
    R.string.yew_mood_romantic to R.string.yew_mood_romantic_query,
    R.string.yew_mood_road to R.string.yew_mood_road_query,
)

internal fun LazyListScope.exploreContent(bridge: MusicHomeBridge) {
    val featuredIndex = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) % Genres.size
    val featured = Genres[featuredIndex]
    val rest = Genres.filterIndexed { i, _ -> i != featuredIndex }

    item(key = "featured") {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) {
            val q = stringResource(featured.query)
            GenreTile(featured, 148.dp, Modifier.fillMaxWidth(), stringResource(R.string.yew_explore_featured)) { bridge.mood(q) }
        }
    }
    item(key = "genres-title") { SectionTitle(stringResource(R.string.yew_explore_genres)) }
    // Mosaico: a wide + narrow row, then narrow + wide, repeating.
    val rows = rest.chunked(2)
    rows.forEachIndexed { index, pair ->
        item(key = "genres-$index") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val wideFirst = index % 2 == 0
                pair.forEachIndexed { i, genre ->
                    val wide = (i == 0) == wideFirst
                    val q = stringResource(genre.query)
                    GenreTile(genre, 96.dp, Modifier.weight(if (pair.size == 1) 1f else if (wide) 1.6f else 1f), null) { bridge.mood(q) }
                }
            }
        }
    }
    item(key = "decades") {
        Column {
            SectionTitle(stringResource(R.string.yew_explore_decades))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Decades, key = { it }) { year ->
                    val shown = if (year >= 2000) year.toString() else (year % 100).toString()
                    val label = stringResource(R.string.yew_explore_decade, shown)
                    val q = stringResource(R.string.yew_explore_decade_q, shown)
                    Pill(label) { bridge.mood(q) }
                }
            }
        }
    }
    item(key = "moods") {
        Column {
            SectionTitle(stringResource(R.string.yew_explore_moods))
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Moods.chunked(3).forEach { line ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        line.forEach { (name, query) ->
                            val q = stringResource(query)
                            Pill(stringResource(name)) { bridge.mood(q) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    BasicText(
        text,
        Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 12.dp).semantics { heading() },
        style = TextStyle(fontFamily = YewFonts.Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = colorResource(R.color.mobile_color_on_surface)),
    )
}

@Composable
private fun GenreTile(genre: Genre, height: Dp, modifier: Modifier, kicker: String?, onClick: () -> Unit) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(genre.color)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column {
            if (kicker != null) {
                BasicText(kicker.uppercase(), style = TextStyle(fontFamily = YewFonts.Body, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp, color = Color(0xFFE6E6EA)))
                Spacer(Modifier.height(4.dp))
            }
            BasicText(
                stringResource(genre.name),
                style = TextStyle(fontFamily = YewFonts.Display, fontWeight = FontWeight.ExtraBold, fontSize = if (kicker != null) 30.sp else 19.sp, color = Color(0xFFF4F4F5)),
            )
        }
    }
}

@Composable
private fun Pill(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colorResource(R.color.mobile_color_pill))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = TextStyle(fontFamily = YewFonts.Body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colorResource(R.color.mobile_color_on_surface)))
    }
}
