package com.newtube.mobile.music

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.liskovsoft.smartyoutubetv2.tv.R
import kotlinx.coroutines.delay

/** YEWMUSIC(compose): o player de música da Proposta 2, instalado sobre o player existente. */
object MusicPlayerUi {
    @JvmStatic
    fun install(view: ComposeView, bridge: MusicPlayerBridge) {
        view.setContent { MusicPlayerScreen(bridge) }
    }
}

private val DefaultTint = Color(0xFF1E3A40)
private val OnTintSecondary = Color(0xFFC9D6D9)

@Composable
fun MusicPlayerScreen(bridge: MusicPlayerBridge) {
    var snap by remember { mutableStateOf(bridge.snapshot()) }
    LaunchedEffect(bridge) {
        while (true) {
            snap = bridge.snapshot()
            delay(250)
        }
    }
    var queueOpen by remember { mutableStateOf(false) }
    QueueBackHandler(queueOpen) { queueOpen = false }

    val cover = rememberCover(snap.coverUrl, snap.fallbackCoverUrl, 720)
    val tint by animateColorAsState(
        targetValue = remember(cover) { cover?.let { tintOf(it) } ?: DefaultTint },
        animationSpec = tween(400),
        label = "tint",
    )
    val density = LocalDensity.current
    val top = with(density) { snap.topInsetPx.toDp() }
    val bottom = with(density) { snap.bottomInsetPx.toDp() }

    Box(Modifier.fillMaxSize().background(tint)) {
        Column(
            Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = top + 4.dp, bottom = bottom + 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(bridge)
            Spacer(Modifier.height(8.dp))
            ModeSwitch(onVideo = bridge::showVideo)
            Spacer(Modifier.height(18.dp))
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val side = if (maxWidth < maxHeight) maxWidth else maxHeight
                CoverArt(cover, snap.title, Modifier.size(side - 8.dp))
            }
            Spacer(Modifier.height(20.dp))
            TitleRow(snap, bridge)
            Spacer(Modifier.height(14.dp))
            SeekBar(snap, bridge)
            Spacer(Modifier.height(8.dp))
            Transport(snap, bridge)
            Spacer(Modifier.height(16.dp))
            ActionBar(snap, bridge, onQueue = { queueOpen = true })
        }
        if (queueOpen) {
            UpNextSheet(bridge, snap.videoId, bottom, onClose = { queueOpen = false })
        }
    }
}

// ------------------------------------------------------------------------------------ pieces

@Composable
private fun TopBar(bridge: MusicPlayerBridge) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconAction(R.drawable.ic_yew_chevron_down, stringResource(R.string.yew_music_minimize), size = 28.dp) { bridge.minimize() }
        BasicText(
            stringResource(R.string.yew_music_now_playing).uppercase(),
            Modifier.weight(1f).semantics { heading() },
            style = label(12.sp, FontWeight.Bold, OnTintSecondary).copy(textAlign = TextAlign.Center, letterSpacing = 0.6.sp),
        )
        IconAction(R.drawable.ic_mobile_more_vert, stringResource(R.string.yew_music_more)) { bridge.openMenu() }
    }
}

@Composable
private fun ModeSwitch(onVideo: () -> Unit) {
    val desc = stringResource(R.string.yew_mode_switch_desc)
    Row(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(YewColors.Glass)
            .border(1.dp, YewColors.GlassLine, RoundedCornerShape(24.dp))
            .padding(4.dp)
            .semantics { contentDescription = desc },
    ) {
        ModeOption(R.drawable.ic_yew_note, stringResource(R.string.yew_mode_music), selected = true) {}
        ModeOption(R.drawable.ic_yew_video, stringResource(R.string.yew_mode_video), selected = false, onClick = onVideo)
    }
}

@Composable
private fun ModeOption(icon: Int, text: String, selected: Boolean, onClick: () -> Unit) {
    val fg = if (selected) Color(0xFF10262A) else YewColors.Text1
    Row(
        Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) YewColors.Marfim else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(painterResource(icon), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(fg))
        BasicText(text, style = label(14.sp, if (selected) FontWeight.Bold else FontWeight.SemiBold, fg))
    }
}

@Composable
private fun CoverArt(cover: Bitmap?, title: String, modifier: Modifier) {
    Box(
        modifier
            .shadow(24.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(YewColors.Surface2),
        contentAlignment = Alignment.Center,
    ) {
        if (cover != null) {
            Image(cover.asImageBitmap(), title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Image(painterResource(R.drawable.ic_yew_note), null, Modifier.size(64.dp), colorFilter = ColorFilter.tint(YewColors.Text3))
        }
    }
}

@Composable
private fun TitleRow(snap: MusicSnapshot, bridge: MusicPlayerBridge) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(
                snap.title,
                Modifier.semantics { heading() },
                style = TextStyle(fontFamily = YewFonts.Display, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = YewColors.Text1, letterSpacing = (-0.5).sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            BasicText(snap.artist, style = label(16.sp, FontWeight.Medium, OnTintSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val likeLabel = stringResource(R.string.yew_music_like)
        val state = stringResource(if (snap.liked) R.string.yew_music_on else R.string.yew_music_off)
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(YewColors.Glass)
                .toggleable(value = snap.liked, role = Role.Switch, onValueChange = { bridge.toggleLike() })
                .semantics { contentDescription = likeLabel; stateDescription = state },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(if (snap.liked) R.drawable.ic_yew_heart_fill else R.drawable.ic_yew_heart), null, Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(if (snap.liked) YewColors.Rubi else YewColors.Text1),
            )
        }
    }
}

@Composable
private fun SeekBar(snap: MusicSnapshot, bridge: MusicPlayerBridge) {
    val duration = snap.durationMs.coerceAtLeast(0L)
    var drag by remember { mutableStateOf<Float?>(null) }
    val played = if (duration > 0) (snap.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val fraction = drag ?: played
    val shownMs = if (drag != null) (fraction * duration).toLong() else snap.positionMs
    val positionLabel = stringResource(R.string.yew_music_position)
    val spoken = stringResource(R.string.yew_music_time_of, spokenTime(shownMs), spokenTime(duration))
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(32.dp)
                .semantics {
                    contentDescription = positionLabel
                    stateDescription = spoken
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                    setProgress { value ->
                        if (duration > 0) bridge.seekTo((value.coerceIn(0f, 1f) * duration).toLong())
                        duration > 0
                    }
                }
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        if (duration > 0) bridge.seekTo(((offset.x / size.width).coerceIn(0f, 1f) * duration).toLong())
                    }
                }
                .pointerInput(duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> drag = (offset.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = {
                            drag?.let { if (duration > 0) bridge.seekTo((it * duration).toLong()) }
                            drag = null
                        },
                        onDragCancel = { drag = null },
                    ) { change, _ ->
                        drag = (change.position.x / size.width).coerceIn(0f, 1f)
                        change.consume()
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            val width = maxWidth
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x40FFFFFF)))
            Box(Modifier.fillMaxWidth(fraction).height(4.dp).clip(RoundedCornerShape(2.dp)).background(YewColors.Text1))
            val thumb: Dp = if (drag != null) 20.dp else 16.dp
            Box(Modifier.offset(x = width * fraction - thumb / 2).size(thumb).clip(CircleShape).background(Color.White))
        }
        Row(Modifier.fillMaxWidth().clearAndSetSemantics {}, horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(clock(shownMs), style = label(13.sp, FontWeight.Medium, OnTintSecondary))
            BasicText(clock(duration), style = label(13.sp, FontWeight.Medium, OnTintSecondary))
        }
    }
}

@Composable
private fun Transport(snap: MusicSnapshot, bridge: MusicPlayerBridge) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        ToggleAction(R.drawable.ic_player_shuffle, stringResource(R.string.yew_music_shuffle), snap.shuffle) { bridge.toggleShuffle() }
        IconAction(R.drawable.ic_player_previous, stringResource(R.string.yew_music_previous), box = 60.dp, size = 36.dp) { bridge.previous() }
        val playLabel = stringResource(if (snap.playing) R.string.yew_music_pause else R.string.yew_music_play)
        Box(
            Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(YewColors.Marfim)
                .clickable(role = Role.Button, onClickLabel = playLabel) { bridge.togglePlayPause() }
                .semantics { contentDescription = playLabel },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(if (snap.playing) R.drawable.ic_player_pause else R.drawable.ic_player_play), null, Modifier.size(36.dp),
                colorFilter = ColorFilter.tint(Color(0xFF10262A)),
            )
        }
        IconAction(R.drawable.ic_player_next, stringResource(R.string.yew_music_next), box = 60.dp, size = 36.dp) { bridge.next() }
        ToggleAction(R.drawable.ic_player_repeat, stringResource(R.string.yew_music_repeat_one), snap.repeatOne) { bridge.toggleRepeat() }
    }
}

@Composable
private fun ActionBar(snap: MusicSnapshot, bridge: MusicPlayerBridge, onQueue: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(YewColors.Glass)
            .border(1.dp, YewColors.GlassLine, RoundedCornerShape(22.dp))
            .padding(vertical = 4.dp),
    ) {
        BarAction(R.drawable.ic_player_queue, stringResource(R.string.yew_music_up_next), Modifier.weight(1f), onQueue)
        BarAction(R.drawable.ic_yew_save, stringResource(R.string.yew_music_save), Modifier.weight(1f)) { bridge.save() }
        BarAction(
            if (snap.downloaded) R.drawable.ic_watch_downloaded else R.drawable.ic_watch_download,
            stringResource(if (snap.downloaded) R.string.yew_music_downloaded else R.string.yew_music_download),
            Modifier.weight(1f),
        ) { bridge.download() }
        BarAction(R.drawable.ic_watch_share, stringResource(R.string.yew_music_share), Modifier.weight(1f)) { bridge.share() }
    }
}

@Composable
private fun BarAction(icon: Int, text: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(icon), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(YewColors.Text1))
        Spacer(Modifier.height(4.dp))
        BasicText(text, style = label(12.sp, FontWeight.SemiBold, YewColors.Text1), maxLines = 1)
    }
}

@Composable
private fun UpNextSheet(bridge: MusicPlayerBridge, videoId: String?, bottomInset: Dp, onClose: () -> Unit) {
    val items = remember(videoId) { bridge.upNext() }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable(onClickLabel = stringResource(R.string.yew_music_close), onClick = onClose))
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(YewColors.Surface1)
                .pointerInput(Unit) { detectTapGestures { } } // taps on the sheet don't reach the scrim
                .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = bottomInset),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(YewColors.Surface2))
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    stringResource(R.string.yew_music_up_next),
                    Modifier.weight(1f).semantics { heading() },
                    style = TextStyle(fontFamily = YewFonts.Display, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = YewColors.Text1),
                )
                IconAction(R.drawable.ic_yew_chevron_down, stringResource(R.string.yew_music_close), tint = YewColors.Text1) { onClose() }
            }
            Spacer(Modifier.height(8.dp))
            if (items.isEmpty()) {
                BasicText(stringResource(R.string.yew_music_up_next_empty), Modifier.padding(end = 12.dp), style = label(15.sp, FontWeight.Normal, YewColors.Text2))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(items) { index, item ->
                        UpNextRow(item) { bridge.playUpNext(index); onClose() }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpNextRow(item: UpNextItem, onClick: () -> Unit) {
    val art = rememberCover(item.coverUrl, null, 160)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(YewColors.Surface2)) {
            if (art != null) Image(art.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 12.dp)) {
            BasicText(item.title, style = label(15.sp, FontWeight.SemiBold, YewColors.Text1), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(item.artist, style = label(13.sp, FontWeight.Normal, YewColors.Text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ------------------------------------------------------------------------------------ helpers

@Composable
private fun IconAction(icon: Int, description: String, box: Dp = 48.dp, size: Dp = 24.dp, tint: Color = YewColors.Text1, onClick: () -> Unit) {
    Box(
        Modifier
            .size(box)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(icon), null, Modifier.size(size), colorFilter = ColorFilter.tint(tint))
    }
}

/** Liga/desliga com o estado também fora da cor: um ponto embaixo do ícone quando ligado. */
@Composable
private fun ToggleAction(icon: Int, description: String, on: Boolean, onToggle: () -> Unit) {
    val state = stringResource(if (on) R.string.yew_music_on else R.string.yew_music_off)
    val color = if (on) YewColors.Rubi else OnTintSecondary
    Column(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .toggleable(value = on, role = Role.Switch, onValueChange = { onToggle() })
            .semantics { contentDescription = description; stateDescription = state },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(icon), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(color))
        Spacer(Modifier.height(3.dp))
        Box(Modifier.size(4.dp).clip(CircleShape).background(if (on) color else Color.Transparent))
    }
}

private fun label(size: TextUnit, weight: FontWeight, color: Color, family: FontFamily = YewFonts.Body) =
    TextStyle(fontFamily = family, fontWeight = weight, fontSize = size, color = color)

private fun clock(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** "1 min 24 s" para o TalkBack, em vez de "1:24". */
private fun spokenTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L)
    val m = total / 60
    val s = total % 60
    return if (m > 0) "$m min $s s" else "$s s"
}

/** O tom da capa, escurecido o bastante para o texto claro passar no contraste AA. */
private fun tintOf(bitmap: Bitmap): Color {
    return try {
        val small = Bitmap.createScaledBitmap(bitmap, 12, 12, true)
        var r = 0L
        var g = 0L
        var b = 0L
        for (x in 0 until 12) for (y in 0 until 12) {
            val p = small.getPixel(x, y)
            r += (p shr 16) and 0xFF
            g += (p shr 8) and 0xFF
            b += p and 0xFF
        }
        val n = 144f
        val k = 0.38f
        Color(red = (r / n) / 255f * k, green = (g / n) / 255f * k, blue = (b / n) / 255f * k)
    } catch (e: RuntimeException) {
        DefaultTint
    }
}

@Composable
private fun rememberCover(url: String?, fallback: String?, sizePx: Int): Bitmap? {
    val context = LocalContext.current.applicationContext
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    DisposableEffect(url, fallback) {
        var active = true
        if (url != null) {
            val target = object : CustomTarget<Bitmap>(sizePx, sizePx) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    if (active) bitmap = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            }
            val glide = Glide.with(context)
            var request = glide.asBitmap().load(url).disallowHardwareConfig()
            if (fallback != null) {
                request = request.error(glide.asBitmap().load(fallback).disallowHardwareConfig())
            }
            request.into(target)
        }
        onDispose { active = false }
    }
    return bitmap
}

/** Com a fila aberta, o Voltar do sistema fecha a fila em vez de minimizar o player. */
@Composable
private fun QueueBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(enabled, activity) {
        val callback = object : OnBackPressedCallback(enabled) {
            override fun handleOnBackPressed() = onBack()
        }
        activity.onBackPressedDispatcher.addCallback(callback)
        onDispose { callback.remove() }
    }
}

private fun android.content.Context.findActivity(): ComponentActivity? {
    var context: android.content.Context? = this
    while (context is android.content.ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}
