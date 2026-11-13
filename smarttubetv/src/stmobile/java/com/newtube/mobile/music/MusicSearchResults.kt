@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.newtube.mobile.music

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
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
 * YEWMUSIC(search): os resultados da Busca na Proposta 2 - "Melhor resultado" em destaque (o
 * primeiro artista entre os primeiros resultados, senão o primeiro item) e o resto em lista de
 * música: capa, título, tipo · artista · duração e o menu de sempre. Mostra a mesma lista que a
 * grade da busca recebe; a paginação continua sendo a da busca.
 */
interface MusicSearchBridge {
    fun open(video: Video)
    fun menu(video: Video)
    fun onNearEnd()
    fun bottomPaddingPx(): Int
}

class MusicSearchState {
    internal var videos by mutableStateOf<List<Video>>(emptyList())

    fun setVideos(list: List<Video>) {
        videos = ArrayList(list)
    }
}

object MusicSearchUi {
    @JvmStatic
    fun install(view: ComposeView, state: MusicSearchState, bridge: MusicSearchBridge) {
        view.setContent { MusicSearchScreen(state, bridge) }
    }
}

private enum class Kind { ARTIST, PLAYLIST, SONG }

private fun kindOf(video: Video): Kind = when {
    video.videoId == null && video.hasChannel() -> Kind.ARTIST
    video.videoId == null && video.hasPlaylist() -> Kind.PLAYLIST
    else -> Kind.SONG
}

@Composable
fun MusicSearchScreen(state: MusicSearchState, bridge: MusicSearchBridge) {
    val videos = state.videos
    val best = remember(videos) { videos.take(6).firstOrNull { kindOf(it) == Kind.ARTIST } ?: videos.firstOrNull() }
    val rest = remember(videos, best) { videos.filter { it !== best } }
    val listState = rememberLazyListState()
    val bottom = with(LocalDensity.current) { bridge.bottomPaddingPx().toDp() }

    LaunchedEffect(listState, videos.size) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 4
        }.distinctUntilChanged().collect { if (it) bridge.onNearEnd() }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(searchThemeColor(R.attr.newtubeColorBackground)),
        state = listState,
        contentPadding = PaddingValues(bottom = bottom + 24.dp),
    ) {
        if (best != null) {
            item(key = "best") { BestResult(best, bridge) }
        }
        if (rest.isNotEmpty()) {
            item(key = "results-title") {
                BasicText(
                    stringResource(R.string.yew_search_results),
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 6.dp).semantics { heading() },
                    style = TextStyle(fontFamily = YewFonts.Display, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colorResource(R.color.mobile_color_on_surface)),
                )
            }
        }
        items(rest, key = { System.identityHashCode(it) }) { video -> ResultRow(video, bridge) }
    }
}

@Composable
private fun BestResult(video: Video, bridge: MusicSearchBridge) {
    val kind = kindOf(video)
    val art = rememberSearchThumb(video, 240)
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
        BasicText(
            stringResource(R.string.yew_search_best),
            Modifier.semantics { heading() },
            style = TextStyle(fontFamily = YewFonts.Display, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colorResource(R.color.mobile_color_on_surface)),
        )
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(searchThemeColor(R.attr.newtubeColorCard))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(art, 72, if (kind == Kind.ARTIST) CircleShape else RoundedCornerShape(10.dp))
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    BasicText(
                        video.getTitle() ?: "",
                        style = TextStyle(fontFamily = YewFonts.Display, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = colorResource(R.color.mobile_color_on_surface)),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BasicText(subtitle(video, kind), style = body(14.sp, colorResource(R.color.mobile_color_on_surface_secondary)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(14.dp))
            val action = when (kind) {
                Kind.ARTIST -> stringResource(R.string.yew_search_open_artist)
                Kind.PLAYLIST -> stringResource(R.string.yew_search_open_playlist)
                Kind.SONG -> stringResource(R.string.yew_music_play)
            }
            Row(
                Modifier
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colorResource(R.color.mobile_color_inverse_surface))
                    .clickable(role = Role.Button) { bridge.open(video) }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (kind == Kind.SONG) {
                    Image(painterResource(R.drawable.ic_player_play), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(colorResource(R.color.mobile_color_on_inverse_surface)))
                }
                BasicText(action, style = TextStyle(fontFamily = YewFonts.Body, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colorResource(R.color.mobile_color_on_inverse_surface)))
            }
        }
    }
}

@Composable
private fun ResultRow(video: Video, bridge: MusicSearchBridge) {
    val kind = kindOf(video)
    val art = rememberSearchThumb(video, 160)
    val more = stringResource(R.string.yew_music_more)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .combinedClickable(role = Role.Button, onLongClickLabel = more, onLongClick = { bridge.menu(video) }) { bridge.open(video) }
            .padding(start = 20.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(art, 52, if (kind == Kind.ARTIST) CircleShape else RoundedCornerShape(8.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
            BasicText(video.getTitle() ?: "", style = TextStyle(fontFamily = YewFonts.Body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colorResource(R.color.mobile_color_on_surface)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(subtitle(video, kind), style = body(13.sp, colorResource(R.color.mobile_color_on_surface_secondary)), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = more) { bridge.menu(video) }
                .semantics { contentDescription = more },
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_mobile_more_vert), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(colorResource(R.color.mobile_color_on_surface_secondary)))
        }
    }
}

@Composable
private fun subtitle(video: Video, kind: Kind): String {
    val type = when (kind) {
        Kind.ARTIST -> stringResource(R.string.yew_search_artist)
        Kind.PLAYLIST -> stringResource(R.string.yew_search_playlist)
        Kind.SONG -> stringResource(R.string.yew_search_song)
    }
    val parts = ArrayList<String>()
    parts.add(type)
    val author = video.getAuthor()
    if (kind != Kind.ARTIST && !author.isNullOrBlank()) parts.add(author)
    val badge = video.badge
    if (kind == Kind.SONG && !badge.isNullOrBlank()) parts.add(badge)
    return parts.joinToString(" · ")
}

@Composable
private fun Cover(art: Bitmap?, size: Int, shape: Shape) {
    Box(Modifier.size(size.dp).clip(shape).background(colorResource(R.color.mobile_color_placeholder))) {
        if (art != null) Image(art.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

private fun body(size: androidx.compose.ui.unit.TextUnit, color: Color) = TextStyle(fontFamily = YewFonts.Body, fontSize = size, color = color)

@Composable
private fun searchThemeColor(attr: Int): Color {
    val context = LocalContext.current
    return remember(context, attr) {
        val values = context.obtainStyledAttributes(intArrayOf(attr))
        val color = values.getColor(0, 0)
        values.recycle()
        Color(color)
    }
}

@Composable
private fun rememberSearchThumb(video: Video, sizePx: Int): Bitmap? {
    val url = video.cardImageUrl ?: video.videoId?.let { "https://i.ytimg.com/vi/$it/mqdefault.jpg" }
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
