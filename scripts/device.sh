#!/usr/bin/env bash
# One shared Android device for everyone working on WormaCeptor: agents and people, from any worktree.
#
#   scripts/device.sh up            boot the emulator headless (or reuse whatever device is attached), wait for boot
#   scripts/device.sh status        the attached device, its Android version and whether it has booted
#   scripts/device.sh install [apk] install and launch the demo app: the given APK, or a fresh debug build of this worktree
#   scripts/device.sh shot [file]   save a PNG screenshot (default: build/device/shot-<time>.png)
#   scripts/device.sh adb <args>    run the SDK's adb against the device
#   scripts/device.sh down          stop the emulator this script started
#
# Always uses the SDK's adb. Homebrew's android-platform-tools is a different adb build, and two
# builds sharing one server kill each other's daemon, which leaves the emulator stuck "offline".
# Env: AVD (default Pixel_10_Pro_XL), ANDROID_HOME (default ~/Library/Android/sdk), ANDROID_SERIAL.
set -euo pipefail

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
ADB="$SDK/platform-tools/adb"
EMULATOR="$SDK/emulator/emulator"
AVD="${AVD:-Pixel_10_Pro_XL}"
APP_ID="com.azikar24.wormaceptorapp"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOG="${TMPDIR:-/tmp}/wormaceptor-emulator.log"

die() { echo "device.sh: $*" >&2; exit 1; }
[[ -x "$ADB" ]] || die "no adb at $ADB (set ANDROID_HOME)"

# The first attached device that adb can talk to, or nothing.
serial() {
	[[ -n "${ANDROID_SERIAL:-}" ]] && { echo "$ANDROID_SERIAL"; return; }
	"$ADB" devices | awk 'NR > 1 && $2 == "device" { print $1; exit }'
}

booted() { [[ "$("$ADB" -s "$1" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; }

up() {
	"$ADB" start-server >/dev/null 2>&1
	local s; s="$(serial)"
	if [[ -z "$s" ]]; then
		# An emulator left "offline" by an adb version clash usually comes back on reconnect.
		if "$ADB" devices | grep -q offline; then "$ADB" reconnect offline >/dev/null 2>&1 || true; sleep 3; s="$(serial)"; fi
	fi
	if [[ -z "$s" ]]; then
		pgrep -f "qemu-system.*-avd $AVD" >/dev/null && die "$AVD is running but adb can't reach it; try: $0 down && $0 up"
		"$EMULATOR" -list-avds | grep -qx "$AVD" || die "no AVD named $AVD (have: $("$EMULATOR" -list-avds | tr '\n' ' '))"
		echo "booting $AVD headless (log: $LOG)"
		nohup "$EMULATOR" -avd "$AVD" -no-window -no-audio -no-boot-anim -no-snapshot-save >"$LOG" 2>&1 &
	fi
	local deadline=$((SECONDS + 300))
	until s="$(serial)"; [[ -n "$s" ]] && booted "$s"; do
		(( SECONDS < deadline )) || die "device not booted after 300 s (see $LOG)"
		"$ADB" devices | grep -q offline && "$ADB" reconnect offline >/dev/null 2>&1 || true
		sleep 3
	done
	status
}

status() {
	local s; s="$(serial)"
	[[ -n "$s" ]] || { echo "no device (run: $0 up)"; return 1; }
	echo "$s  Android $("$ADB" -s "$s" shell getprop ro.build.version.release | tr -d '\r')  $("$ADB" -s "$s" shell getprop ro.product.model | tr -d '\r')  booted=$(booted "$s" && echo yes || echo no)"
}

install() {
	local s; s="$(serial)"; [[ -n "$s" ]] || die "no device (run: $0 up)"
	local apk="${1:-}"
	if [[ -z "$apk" ]]; then
		(cd "$ROOT" && ./gradlew -q :app:assembleDebug) || die "debug build failed (see above); pass a built APK instead"
		apk="$(find "$ROOT/app/build/outputs/apk/debug" -name '*.apk' -print | head -1)"
	fi
	[[ -f "$apk" ]] || die "no APK at ${apk:-app/build/outputs/apk/debug}"
	"$ADB" -s "$s" install -r -t "$apk" >/dev/null
	"$ADB" -s "$s" shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
	echo "installed and launched $(basename "$apk") on $s"
}

shot() {
	local s; s="$(serial)"; [[ -n "$s" ]] || die "no device (run: $0 up)"
	local out="${1:-$ROOT/build/device/shot-$(date +%Y%m%d-%H%M%S).png}"
	mkdir -p "$(dirname "$out")"
	"$ADB" -s "$s" exec-out screencap -p >"$out"
	[[ -s "$out" ]] || die "empty screenshot"
	echo "$out"
}

down() {
	local s
	for s in $("$ADB" devices | awk 'NR > 1 && $1 ~ /^emulator-/ { print $1 }'); do "$ADB" -s "$s" emu kill >/dev/null 2>&1 || true; done
	echo "emulator stopped"
}

case "${1:-status}" in
	up) up ;;
	status) status ;;
	install) shift; install "${1:-}" ;;
	shot) shift; shot "${1:-}" ;;
	adb) shift; s="$(serial)"; exec "$ADB" ${s:+-s "$s"} "$@" ;;
	down) down ;;
	*) sed -n '2,13p' "$0"; exit 2 ;;
esac
