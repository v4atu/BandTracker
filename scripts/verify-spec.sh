#!/usr/bin/env bash
# Verifica que el proyecto cumple lo verificable de SPEC.md §3, §6 y §8
# (criterios de aceptación 1 y 2, reglas de privacidad e i18n).
# Uso: scripts/verify-spec.sh [--with-unit-tests]   (desde cualquier carpeta)
#   --with-unit-tests  corre además las pruebas JVM (:app:testDebugUnitTest). Las pruebas
#                      instrumentadas necesitan un dispositivo y se corren aparte
#                      (./gradlew :app:connectedDebugAndroidTest).
set -u

WITH_UNIT_TESTS=0
for arg in "$@"; do
  case "$arg" in
    --with-unit-tests) WITH_UNIT_TESTS=1 ;;
    *) echo "Opción desconocida: $arg"; exit 2 ;;
  esac
done

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT" || exit 2

FAILURES=0
pass() { echo "  OK   $1"; }
fail() { echo "  FAIL $1"; FAILURES=$((FAILURES + 1)); }

# --- Localizar aapt2 a partir de sdk.dir en local.properties -----------------
SDK_DIR="$(sed -n 's/^sdk\.dir=//p' local.properties 2>/dev/null | head -n1)"
AAPT2=""
if [ -n "$SDK_DIR" ] && [ -d "$SDK_DIR/build-tools" ]; then
  AAPT2="$(ls -d "$SDK_DIR"/build-tools/*/aapt2 2>/dev/null | sort -V | tail -n1)"
fi
[ -n "$AAPT2" ] || { echo "No se encontró aapt2 (revisa sdk.dir en local.properties)"; exit 2; }

echo "== Compilación =="
if ./gradlew --console=plain -q :app:assembleDebug; then
  pass "assembleDebug compila"
else
  fail "assembleDebug no compila"
fi

APK="$(ls app/build/outputs/apk/debug/*.apk 2>/dev/null | head -n1)"

echo "== Privacidad (SPEC §8) =="
if [ -n "$APK" ]; then
  PERMS="$("$AAPT2" dump permissions "$APK" 2>&1 | grep -E '^uses-permission|android.permission' || true)"
  if [ -z "$PERMS" ]; then pass "el APK no declara permisos"; else fail "el APK declara permisos: $PERMS"; fi

  if "$AAPT2" dump xmltree --file AndroidManifest.xml "$APK" 2>&1 | grep -qi 'INTERNET'; then
    fail "el manifest del APK menciona INTERNET"
  else
    pass "el manifest del APK no menciona INTERNET"
  fi

  BACKUP="$("$AAPT2" dump xmltree --file AndroidManifest.xml "$APK" 2>&1 | grep 'allowBackup' || true)"
  if echo "$BACKUP" | grep -Eq '=\(type 0x12\)0x0|=false'; then
    pass "allowBackup=false"
  else
    fail "allowBackup no es false (${BACKUP:-no declarado})"
  fi
else
  fail "no hay APK debug para inspeccionar"
fi

if grep -qi 'INTERNET' app/src/main/AndroidManifest.xml; then
  fail "app/src/main/AndroidManifest.xml menciona INTERNET"
else
  pass "el manifest fuente no menciona INTERNET"
fi

DEPS="$(./gradlew --console=plain -q :app:dependencies --configuration releaseRuntimeClasspath 2>&1)"
mkdir -p build && echo "$DEPS" > build/dependency-tree.txt
echo "  (árbol de dependencias completo en build/dependency-tree.txt)"
if echo "$DEPS" | grep -Eiq 'com\.google\.android\.gms|com\.google\.firebase|analytics|crashlytics|okhttp'; then
  fail "dependencias prohibidas en releaseRuntimeClasspath (gms/firebase/analytics/crashlytics/okhttp)"
else
  pass "sin gms/firebase/analytics/crashlytics/okhttp en releaseRuntimeClasspath"
fi

echo "== Stack (SPEC §3) =="
echo "$DEPS" | grep -q 'androidx.room3:room3-runtime' && pass "Room 3 presente" || fail "falta androidx.room3:room3-runtime"
echo "$DEPS" | grep -q 'org.jetbrains.kotlinx:kotlinx-serialization-json' && pass "kotlinx-serialization-json presente" || fail "falta kotlinx-serialization-json"

echo "== Idiomas (SPEC §6) =="
ES="app/src/main/res/values/strings.xml"
EN="app/src/main/res/values-en/strings.xml"
if [ -f "$ES" ] && [ -f "$EN" ]; then
  if diff <(grep -o 'name="[^"]*"' "$ES" | sort) <(grep -o 'name="[^"]*"' "$EN" | sort) >/dev/null; then
    pass "values y values-en tienen las mismas claves"
  else
    fail "values y values-en tienen claves distintas"
  fi
else
  fail "falta $ES o $EN"
fi

echo "== Licencias de terceros (DESIGN §3 y §5) =="
if [ -n "$APK" ]; then
  APK_FILES="$(unzip -Z1 "$APK" 2>/dev/null)"
  for lic in assets/licenses/istok-web-OFL.txt assets/licenses/lucide-LICENSE.txt; do
    if echo "$APK_FILES" | grep -qx "$lic"; then pass "el APK incluye $lic"; else fail "el APK no incluye $lic"; fi
  done
  for lic in istok-web-OFL.txt lucide-LICENSE.txt; do
    if cmp -s "docs/licenses/$lic" "app/src/main/assets/licenses/$lic"; then
      pass "$lic idéntico a docs/licenses"
    else
      fail "$lic difiere entre docs/licenses y app/src/main/assets/licenses"
    fi
  done
else
  fail "no hay APK debug para inspeccionar las licencias"
fi

echo "== Licencia (SPEC §3) =="
if [ -f LICENSE ] && grep -q 'MIT License' LICENSE && grep -q 'Permission is hereby granted' LICENSE; then
  pass "LICENSE MIT presente"
else
  fail "falta LICENSE con texto MIT"
fi

if [ "$WITH_UNIT_TESTS" -eq 1 ]; then
  echo "== Pruebas JVM =="
  if ./gradlew --console=plain -q :app:testDebugUnitTest; then
    pass "pruebas JVM en verde"
  else
    fail "fallan pruebas JVM (ver app/build/reports/tests/testDebugUnitTest)"
  fi
fi

echo
if [ "$FAILURES" -eq 0 ]; then echo "Todo en verde."; else echo "$FAILURES verificación(es) fallaron."; fi
exit $([ "$FAILURES" -eq 0 ] && echo 0 || echo 1)
