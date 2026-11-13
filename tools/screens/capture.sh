#!/usr/bin/env bash
# Capturas das telas do YewMusic num emulador (workflow yewmusic-telas.yml).
# Navega pelo texto dos botões (uiautomator), não por coordenadas, e salva PNGs em build/screens.
# Nunca falha o job por uma tela que não abriu: a captura que faltar já diz o que houve.
set -u
PKG=com.yewmusic.debug  # o build de debug tem o sufixo .debug
DIR=smarttubetv/build/outputs/renamed_apks/stmobileDebug
APK=$(ls "$DIR"/*universal*.apk 2>/dev/null | head -1)
[ -z "$APK" ] && APK=$(ls "$DIR"/*.apk | head -1)
OUT=build/screens
mkdir -p "$OUT"

shot() { sleep "${2:-3}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "captura: $1"; report; }

# Toca no elemento cujo texto é $1. $2=last escolhe o mais baixo da tela (rótulos da barra).
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  local xy
  xy=$(python3 - "$1" "${2:-first}" <<'PY'
import re, sys
text, which = sys.argv[1], sys.argv[2]
xml = open('/tmp/ui.xml', encoding='utf-8').read()
hits = []
for node in re.findall(r'<node [^>]*>', xml):
    if 'text="%s"' % text in node:
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
        if b:
            x1, y1, x2, y2 = map(int, b.groups())
            hits.append(((x1 + x2) // 2, (y1 + y2) // 2))
if hits:
    x, y = max(hits, key=lambda h: h[1]) if which == 'last' else hits[0]
    print(x, y)
PY
)
  if [ -n "$xy" ]; then adb shell input tap $xy; echo "toque: $1 ($xy)"; else echo "não achei: $1"; fi
}

# Toca no elemento cuja descrição de acessibilidade é $1 (botões só com ícone).
tap_desc() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  local xy
  xy=$(python3 - "$1" <<'PY'
import re, sys
desc = sys.argv[1]
xml = open('/tmp/ui.xml', encoding='utf-8').read()
for node in re.findall(r'<node [^>]*>', xml):
    if 'content-desc="%s"' % desc in node:
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
        if b:
            x1, y1, x2, y2 = map(int, b.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            break
PY
)
  if [ -n "$xy" ]; then adb shell input tap $xy; echo "toque: $1 ($xy)"; else echo "não achei: $1"; fi
}

launch() {
  adb shell am force-stop $PKG
  adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
}

# O que está na frente, e qualquer falha do app desde a última chamada.
report() {
  echo "frente: $(adb shell dumpsys activity activities | grep -m1 -E 'topResumedActivity|mResumedActivity' | tr -s ' ')"
  adb logcat -d -b crash | tail -60
  adb logcat -d -s AndroidRuntime:E | tail -60
  adb logcat -c
}

adb wait-for-device
# Português do Brasil no aparelho todo (o app segue o idioma do sistema por padrão).
adb root >/dev/null 2>&1; sleep 3; adb wait-for-device
adb shell "setprop persist.sys.locale pt-BR; setprop ctl.restart zygote"
sleep 20; adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
sleep 10
adb shell settings put system screen_off_timeout 1800000
adb shell svc power stayon true
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard >/dev/null 2>&1

adb install -r -g "$APK"
adb logcat -c
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS >/dev/null 2>&1

# --- Tema escuro ---
adb shell cmd uimode night yes
launch
shot 01_inicio 30
adb shell input swipe 540 1800 540 900 900; shot 01b_inicio_rolado 3
tap_text "Explorar" last; shot 02_explorar 8
tap_text "Biblioteca" last; shot 03_biblioteca 4
tap_text "Baixadas"; shot 03b_downloads 4
adb shell input keyevent KEYCODE_BACK; sleep 2
tap_desc "Configurações"; shot 04_configuracoes 5
adb shell input swipe 540 1900 540 700 900; shot 04b_configuracoes_baixo 3
tap_text "Sobre"; shot 04c_sobre 4
adb shell input keyevent KEYCODE_BACK; sleep 2
adb shell input keyevent KEYCODE_BACK; sleep 2
tap_text "Busca" last; shot 05_busca 5
adb shell input keyevent KEYCODE_BACK; sleep 2
tap_text "Início" last; sleep 2
adb shell am start -a android.intent.action.VIEW -d "https://www.youtube.com/watch?v=jNQXAC9IVRw" $PKG >/dev/null 2>&1
shot 06_video 30
adb shell input swipe 540 1800 540 900 900; shot 06b_video_baixo 3
adb shell input keyevent KEYCODE_BACK; shot 07_mini 6

# --- Tema claro ---
adb shell cmd uimode night no
launch
shot 08_inicio_claro 25
tap_text "Biblioteca" last; shot 09_biblioteca_claro 5
tap_desc "Configurações"; shot 10_configuracoes_claro 5
exit 0
