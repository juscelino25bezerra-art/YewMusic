package com.newtube.mobile.music

/**
 * YEWMUSIC(compose): o que a tela de música precisa do player que já existe
 * (MobilePlaybackActivity). A tela só lê um retrato do estado e pede ações; quem toca, troca de
 * música, curte e baixa continua sendo o código do player, sem nada duplicado.
 */
interface MusicPlayerBridge {
    fun snapshot(): MusicSnapshot

    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun toggleLike()
    fun toggleShuffle()
    fun toggleRepeat()

    /** Fecha a tela para o mini-player, como o botão Voltar. */
    fun minimize()
    fun openMenu()

    /** Troca para o modo Vídeo (o player de vídeo de sempre). */
    fun showVideo()
    fun save()
    fun download()
    fun share()

    fun upNext(): List<UpNextItem>
    fun playUpNext(index: Int)
}

data class MusicSnapshot(
    val videoId: String?,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val fallbackCoverUrl: String?,
    val positionMs: Long,
    val durationMs: Long,
    val playing: Boolean,
    val liked: Boolean,
    val shuffle: Boolean,
    val repeatOne: Boolean,
    val downloaded: Boolean,
    val topInsetPx: Int,
    val bottomInsetPx: Int,
)

data class UpNextItem(
    val title: String,
    val artist: String,
    val coverUrl: String?,
)
