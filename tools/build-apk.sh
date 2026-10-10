#!/usr/bin/env bash
# PinBom · one-shot APK build on a bare sandbox.
# Toolchain lives in /opt (outside the persisted workspace); only the APK is copied back.
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive

REPO=/home/user/persian-pintrest
SDK=/opt/tc/sdk
GRADLE=/opt/tc/gradle
export GRADLE_USER_HOME=/opt/gradle-home
export ANDROID_HOME=$SDK
export ANDROID_SDK_ROOT=$SDK
sudo mkdir -p /opt/tc /opt/gradle-home /opt/gradle-project-cache
sudo chown -R "$(id -u):$(id -g)" /opt/tc /opt/gradle-home /opt/gradle-project-cache
mkdir -p /opt/tc "$GRADLE_USER_HOME"

echo "###STAGE 1 · JDK"
if [ ! -x /usr/lib/jvm/java-21-openjdk-amd64/bin/javac ]; then
  sudo apt-get update -qq
  sudo apt-get install -y -qq openjdk-21-jdk-headless
fi
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version 2>&1 | head -2

echo "###STAGE 2 · swap (the box has 1.9 GB RAM; kapt + Compose need headroom)"
if sudo swapon --show=NAME 2>/dev/null | grep -q /swapfile; then
  echo "swap already active"
elif [ -f /swapfile ]; then
  sudo swapon /swapfile 2>/dev/null || true
else
  sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile >/dev/null && sudo swapon /swapfile
fi
swapon --show 2>/dev/null || sudo swapon --show || true
free -m | head -3

echo "###STAGE 3 · Gradle 9.6"
if [ ! -x "$GRADLE/bin/gradle" ]; then
  curl -sSfL -o /opt/tc/gradle.zip https://services.gradle.org/distributions/gradle-9.6.0-bin.zip
  ( cd /opt/tc && unzip -q -o gradle.zip && mv -f gradle-9.6.0 "$GRADLE" )
fi
"$GRADLE/bin/gradle" --version | head -6

echo "###STAGE 4 · Android SDK"
if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  curl -sSfL -o /opt/tc/clt.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  mkdir -p "$SDK/cmdline-tools"
  ( cd "$SDK/cmdline-tools" && unzip -q -o /opt/tc/clt.zip && mv -f cmdline-tools latest )
fi
SDKM="$SDK/cmdline-tools/latest/bin/sdkmanager"
yes | "$SDKM" --licenses >/dev/null 2>&1 || true
"$SDKM" "platform-tools" "platforms;android-35" "platforms;android-37.0" "build-tools;35.0.0" "build-tools;37.0.0" 2>&1 | tail -3
"$SDKM" "build-tools;36.0.0" 2>&1 | tail -2 || true
ls "$SDK"

echo "###STAGE 4.5 · restore the wrapper the repo is missing (gradlew + jar)"
cd "$REPO"
"$GRADLE/bin/gradle" wrapper --gradle-version 9.6.0 --no-daemon --console=plain \
  -Dorg.gradle.jvmargs="-Xmx1280m" 2>&1 | tail -5 || echo "wrapper task failed (continuing)"
ls -la gradlew gradle/wrapper/ 2>/dev/null || true

echo "###STAGE 5 · gradle :app:assembleDebug"
cd "$REPO"
set -o pipefail
"$GRADLE/bin/gradle" :app:assembleDebug \
  --no-daemon --console=plain --no-watch-fs \
  --project-cache-dir=/opt/gradle-project-cache \
  -Dorg.gradle.jvmargs="-Xmx2560m -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8" \
  -Dorg.gradle.workers.max=1 -Dorg.gradle.parallel=false \
  -Dkotlin.compiler.execution.strategy=in-process \
  --stacktrace 2>&1 | tee /opt/tc/build.log

echo "###STAGE 6 · collect the APK"
ls -la "$REPO/app/build/outputs/apk/debug/"
cp -f "$REPO"/app/build/outputs/apk/debug/*.apk /home/user/pinbom-1.0.0-debug.apk
ls -la /home/user/pinbom-1.0.0-debug.apk
echo "###DONE"
