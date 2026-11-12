package com.newtube.mobile.ui.playback;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * YEWMUSIC(player): o seletor Música | Vídeo do player. Em Música o vídeo não é baixado nem
 * decodificado (a mesma trava de faixa do áudio em segundo plano) e a capa ocupa o lugar do vídeo,
 * no player e no mini-player. É o padrão do YewMusic; a escolha vale para todos os vídeos.
 */
public final class MusicMode {
    private static final String PREFS = "yewmusic_player";
    private static final String KEY_MUSIC = "music_mode";

    private MusicMode() {
    }

    public static boolean isOn(Context context) {
        return prefs(context).getBoolean(KEY_MUSIC, true);
    }

    public static void set(Context context, boolean music) {
        prefs(context).edit().putBoolean(KEY_MUSIC, music).apply();
    }

    /** A capa de um vídeo do YouTube, em 16:9 sem as faixas pretas do hqdefault. */
    public static String coverUrl(com.liskovsoft.smartyoutubetv2.common.app.models.data.Video video) {
        if (video == null) {
            return null;
        }
        if (video.videoId != null) {
            return "https://i.ytimg.com/vi/" + video.videoId + "/maxresdefault.jpg";
        }
        return video.cardImageUrl;
    }

    /** Reserva quando o vídeo não tem a versão maxres. */
    public static String fallbackCoverUrl(com.liskovsoft.smartyoutubetv2.common.app.models.data.Video video) {
        if (video == null) {
            return null;
        }
        if (video.videoId != null) {
            return "https://i.ytimg.com/vi/" + video.videoId + "/mqdefault.jpg";
        }
        return video.cardImageUrl;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
