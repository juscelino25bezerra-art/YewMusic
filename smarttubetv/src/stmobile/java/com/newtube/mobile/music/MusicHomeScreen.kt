@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.newtube.mobile.music

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video
import com.liskovsoft.smartyoutubetv2.tv.R
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * YEWMUSIC(compose): o Início da Proposta 2. Mostra a mesma lista que o feed de sempre recebe
 * (o presenter, o cache e a paginação continuam os mesmos), agrupada em prateleiras com título.
 */
interface MusicHomeBridge {
    fun open(video: Video)
    fun menu(video: Video)
    fun recent(): List<Video>
    fun onNearEnd()
    fun mood(query: String)
    fun bottomPaddingPx(): Int
}

/** O estado que o Browse atualiza a cada lista nova do feed. */
class MusicHomeState {
    internal var videos by mutableStateOf<List<Video>>(emptyList())

    /** Explorar usa a mesma tela, sem os humores e sem Ouvir de novo. */
    internal var explore by mutableStateOf(false)

    fun setExplore(value: Boolean) {
        explore = value
    }

    fun setVideos(list: List<Video>) {
        videos = ArrayList(list)
    }
}

object MusicHomeUi {
    @JvmStatic
    fun install(view: ComposeView, state: MusicHomeState, bridge: MusicHomeBridge) {
        view.setContent { MusicHomeScreen(state, bridge) }
    }
}

private class Shelf(val key: String, val title: String?, val videos: List<Video>)

private fun shelvesOf(videos: List<Video>): List<Shelf> {
    val result = ArrayList<Shelf>()
    var current: ArrayList<Video>? = null
    var currentGroup: Any? = Unit
    var title: String? = null
    for (video in videos) {
        val group = video.getGroup()
        if (current == null || group !== currentGroup) {
            if (current != null && current.isNotEmpty()) {
                result.add(Shelf("s${result.size}", title, current))
            }
            current = ArrayList()
            currentGroup = group
            title = group?.getTitle()
        }
        current.add(video)
    }
    if (current != null && current.isNotEmpty()) {
        result.add(Shelf("s${result.size}", title, current))
    }
    return result
}

@Composable
fun MusicHomeScreen(state: MusicHomeState, bridge: MusicHomeBridge) {
    val videos = state.videos
    val shelves = remember(videos) { shelvesOf(videos) }
    val explore = state.explore
    val recent = remember(videos, explore) { if (explore) emptyList() else bridge.recent().take(6) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val bottom = with(density) { bridge.bottomPaddingPx().toDp() }

    LaunchedEffect(listState, shelves.size) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 2
        }.distinctUntilChanged().collect { nearEnd -> if (nearEnd) bridge.onNearEnd() }
    }

    val forYou = stringResource(R.string.yew_home_for_you)
    val homeTitle = stringResource(R.string.header_home)
    LazyColumn(
        Modifier.fillMaxSize().background(themeColor(R.attr.newtubeColorBackground)),
        state = listState,
        contentPadding = PaddingValues(bottom = bottom + 24.dp),
    ) {
        if (explore) {
            exploreContent(bridge)
            return@LazyColumn
        }
        item(key = "moods") { MoodChips(bridge) }
        if (recent.isNotEmpty()) {
            item(key = "recent") {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp)) {
                    ShelfTitle(stringResource(R.string.yew_home_listen_again))
                    Spacer(Modifier.height(12.dp))
                    recent.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { RecentTile(it, bridge, Modifier.weight(1f)) }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        items(shelves, key = { it.key }) { shelf ->
            Column(Modifier.padding(top = 22.dp)) {
                Box(Modifier.padding(horizontal = 20.dp)) { ShelfTitle(shelf.title?.takeIf { it.isNotBlank() && it != homeTitle } ?: forYou) }
                Spacer(Modifier.height(12.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(shelf.videos, key = { System.identityHashCode(it) }) { video -> ShelfCard(video, bridge) }
                }
            }
        }
    }
}

@Composable
private fun MoodChips(bridge: MusicHomeBridge) {
    val moods = listOf(
        stringResource(R.string.yew_home_for_you) to null,
        stringResource(R.string.yew_mood_relax) to stringResource(R.string.yew_mood_relax_query),
        stringResource(R.string.yew_mood_energy) to stringResource(R.string.yew_mood_energy_query),
        stringResource(R.string.yew_mood_focus) to stringResource(R.string.yew_mood_focus_query),
        stringResource(R.string.yew_mood_workout) to stringResource(R.string.yew_mood_workout_query),
    )
    LazyRow(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(moods, key = { it.first }) { (label, query) ->
            val selected = query == null
            val bg = if (selected) colorResource(R.color.mobile_color_inverse_surface) else themeColor(R.attr.newtubeColorCard)
            val fg = if (selected) colorResource(R.color.mobile_color_on_inverse_surface) else colorResource(R.color.mobile_color_on_surface)
            Box(
                Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .clickable(role = Role.Tab) { if (query != null) bridge.mood(query) }
                    .semantics { this.selected = selected }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(label, style = TextStyle(fontFamily = YewFonts.Body, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp, color = fg))
            }
        }
    }
}

@Composable
private fun ShelfTitle(text: String) {
    BasicText(
        text,
        Modifier.semantics { heading() },
        style = TextStyle(fontFamily = YewFonts.Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.3).sp, color = colorResource(R.color.mobile_color_on_surface)),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun RecentTile(video: Video, bridge: MusicHomeBridge, modifier: Modifier) {
    val art = rememberThumb(video, 180)
    val more = stringResource(R.string.yew_music_more)
    Row(
        modifier
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(themeColor(R.attr.newtubeColorCard))
            .combinedClickable(role = Role.Button, onLongClickLabel = more, onLongClick = { bridge.menu(video) }) { bridge.open(video) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(60.dp).background(colorResource(R.color.mobile_color_placeholder))) {
            if (art != null) Image(art.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            BasicText(video.getTitle() ?: "", style = TextStyle(fontFamily = YewFonts.Body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colorResource(R.color.mobile_color_on_surface)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(video.getAuthor() ?: "", style = TextStyle(fontFamily = YewFonts.Body, fontSize = 12.sp, color = colorResource(R.color.mobile_color_on_surface_secondary)), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ShelfCard(video: Video, bridge: MusicHomeBridge) {
    val art = rememberThumb(video, 400)
    val more = stringResource(R.string.yew_music_more)
    Column(
        Modifier
            .width(152.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(role = Role.Button, onLongClickLabel = more, onLongClick = { bridge.menu(video) }) { bridge.open(video) },
    ) {
        Box(Modifier.size(152.dp).clip(RoundedCornerShape(14.dp)).background(colorResource(R.color.mobile_color_placeholder))) {
            if (art != null) Image(art.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.height(8.dp))
        BasicText(video.getTitle() ?: "", style = TextStyle(fontFamily = YewFonts.Body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colorResource(R.color.mobile_color_on_surface)), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        BasicText(video.getAuthor() ?: "", style = TextStyle(fontFamily = YewFonts.Body, fontSize = 13.sp, color = colorResource(R.color.mobile_color_on_surface_secondary)), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun thumbUrl(video: Video): String? =
    video.cardImageUrl ?: video.videoId?.let { "https://i.ytimg.com/vi/$it/mqdefault.jpg" }

@Composable
private fun rememberThumb(video: Video, sizePx: Int): Bitmap? {
    val url = thumbUrl(video)
    val context = LocalContext.current
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    DisposableEffect(url) {
        var active = true
        if (url != null) {
            Glide.with(context.applicationContext).asBitmap().load(url).into(object : CustomTarget<Bitmap>(sizePx, sizePx) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    if (active) bitmap = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
        }
        onDispose { active = false }
    }
    return bitmap
}

/** A cor de um atributo do tema do app (assim o fundo preto puro, quando ligado, também vale aqui). */
@Composable
private fun themeColor(attr: Int): Color {
    val context = LocalContext.current
    return remember(context, attr) {
        val values = context.obtainStyledAttributes(intArrayOf(attr))
        val color = values.getColor(0, 0)
        values.recycle()
        Color(color)
    }
}
