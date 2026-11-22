#!/usr/bin/env bash
# 构建「无障碍锁」APK。
#
# 依赖：JDK 17+、Android SDK（platform android-34 + build-tools，需含 aapt2 / apksigner / zipalign）。
# 可选环境变量：
#   ANDROID_JAR    android.jar 路径（默认取 $ANDROID_HOME/platforms/android-34/android.jar）
#   BUILD_TOOLS    build-tools 目录（默认自动探测 PATH 或 $ANDROID_HOME）
#   ALOCK_KS       签名库路径（默认 ./ks.jks，不存在会自动生成一个开发用签名）
#   ALOCK_KS_PASS  签名库口令（默认 alock2024，仅本地开发用）
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP="$ROOT/app"
OUT="$ROOT/build"

SDK_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/lib/android-sdk}}"

# 版本号（每次发布改这里）
VER_CODE="${VER_CODE:-7}"
VER_NAME="${VER_NAME:-1.3.3}"

if [ -z "${ANDROID_JAR:-}" ]; then
  for p in "$SDK_HOME/platforms/android-34/android.jar" \
           "$SDK_HOME/platforms/android-33/android.jar" \
           "$SDK_HOME/platforms"/android-*/android.jar \
           "$ROOT/android.jar"; do
    if [ -f "$p" ]; then ANDROID_JAR="$p"; break; fi
  done
fi
[ -f "${ANDROID_JAR:-}" ] || { echo "找不到 android.jar，请设置 ANDROID_JAR" >&2; exit 1; }

if [ -z "${BUILD_TOOLS:-}" ]; then
  if [ -d "$SDK_HOME/build-tools" ]; then
    BUILD_TOOLS="$(ls -d "$SDK_HOME"/build-tools/*/ 2>/dev/null | sort -V | tail -1)"
    BUILD_TOOLS="${BUILD_TOOLS%/}"
  elif command -v aapt2 >/dev/null 2>&1; then
    BUILD_TOOLS="$(dirname "$(command -v aapt2)")"
  fi
fi
[ -x "${BUILD_TOOLS:-}/aapt2" ] || { echo "找不到 aapt2，请设置 BUILD_TOOLS=/path/to/build-tools/x.y.z" >&2; exit 1; }

ALOCK_KS="${ALOCK_KS:-$ROOT/ks.jks}"
ALOCK_KS_PASS="${ALOCK_KS_PASS:-alock2024}"

rm -rf "$OUT"
mkdir -p "$OUT/compiled" "$OUT/gen" "$OUT/classes" "$OUT/dex"

echo "== 1/6 编译资源 =="
"$BUILD_TOOLS/aapt2" compile --dir "$APP/res" -o "$OUT/compiled/res.zip"

echo "== 2/6 链接清单与资源 =="
"$BUILD_TOOLS/aapt2" link \
  -o "$OUT/base.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$APP/AndroidManifest.xml" \
  -R "$OUT/compiled/res.zip" \
  --java "$OUT/gen" \
  --min-sdk-version 24 \
  --target-sdk-version 33 \
  --version-code "$VER_CODE" \
  --version-name "$VER_NAME" \
  --auto-add-overlay

echo "== 2.5/6 Shizuku 依赖 + AIDL =="
DEPS="$OUT/deps"
mkdir -p "$DEPS"
SZ_VER=13.1.5
for mod in api provider; do
  if [ ! -f "$DEPS/$mod.jar" ]; then
    curl -sSL --max-time 180 -o "$DEPS/$mod.aar" \
      "https://repo1.maven.org/maven2/dev/rikka/shizuku/$mod/$SZ_VER/$mod-$SZ_VER.aar"
    (cd "$DEPS" && unzip -o -q "$mod.aar" classes.jar && mv -f classes.jar "$mod.jar" && rm -f "$mod.aar")
  fi
done
mkdir -p "$OUT/gen/aidl"
for f in $(find "$ROOT/third_party/shizuku/aidl" -name '*.aidl'); do
  # 注意：aidl 的 -o 是指目录，且一次只喂一个文件，否则会把下一个输入当成输出覆盖掉
  "$BUILD_TOOLS/aidl" -I "$ROOT/third_party/shizuku/aidl" -o "$OUT/gen/aidl" "$f"
done

echo "== 3/6 编译 Java =="
javac -encoding UTF-8 --release 11 \
  -classpath "$ANDROID_JAR:$DEPS/api.jar:$DEPS/provider.jar" \
  -d "$OUT/classes" \
  $(find "$APP/java" "$OUT/gen" -name '*.java')

echo "== 4/6 转 dex =="
if [ -x "$BUILD_TOOLS/d8" ]; then
  "$BUILD_TOOLS/d8" --release --min-api 24 --lib "$ANDROID_JAR" \
    --output "$OUT/dex" "$DEPS/api.jar" "$DEPS/provider.jar" $(find "$OUT/classes" -name '*.class')
else
  R8="${R8_JAR:-$ROOT/r8.jar}"
  [ -f "$R8" ] || { echo "没有 d8，也没有 r8.jar（可用 R8_JAR 指定）" >&2; exit 1; }
  java -cp "$R8" com.android.tools.r8.D8 --release --min-api 24 --lib "$ANDROID_JAR" \
    --output "$OUT/dex" "$DEPS/api.jar" "$DEPS/provider.jar" $(find "$OUT/classes" -name '*.class')
fi

echo "== 5/6 打包并对齐 =="
cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && zip -j -0 -q ../unsigned.apk classes.dex)
"$BUILD_TOOLS/zipalign" -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

echo "== 6/6 签名 =="
if [ ! -f "$ALOCK_KS" ]; then
  echo "  未找到签名库，生成一个开发用签名（发布前请换成自己的）"
  keytool -genkeypair -keystore "$ALOCK_KS" -alias alock \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$ALOCK_KS_PASS" -keypass "$ALOCK_KS_PASS" \
    -dname "CN=Accessibility Lock, O=alaslock, C=CN" >/dev/null 2>&1
fi
APKSIGNER="$BUILD_TOOLS/apksigner"
[ -x "$APKSIGNER" ] || APKSIGNER="apksigner"

"$APKSIGNER" sign \
  --ks "$ALOCK_KS" --ks-pass "pass:$ALOCK_KS_PASS" --key-pass "pass:$ALOCK_KS_PASS" \
  --v1-signing-enabled true --v2-signing-enabled true \
  --out "$OUT/alock.apk" "$OUT/aligned.apk"

echo "== 校验 =="
"$APKSIGNER" verify --print-certs "$OUT/alock.apk" | head -3
"$BUILD_TOOLS/aapt2" dump badging "$OUT/alock.apk" | head -2
ls -lh "$OUT/alock.apk"
