#!/usr/bin/env bash
# PinBom · diagnostic harness on a bare sandbox: install the toolchain, then run
# the Robolectric smoke test that launches the real MainActivity.
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive
REPO=/home/user/persian-pintrest
SDK=/opt/tc/sdk
GRADLE=/opt/tc/gradle
export GRADLE_USER_HOME=/opt/gradle-home
export ANDROID_HOME=$SDK ANDROID_SDK_ROOT=$SDK
sudo mkdir -p /opt/tc /opt/gradle-home /opt/gradle-project-cache
sudo chown -R "$(id -u):$(id -g)" /opt/tc /opt/gradle-home /opt/gradle-project-cache
mkdir -p /opt/tc "$GRADLE_USER_HOME"

echo "###STAGE 1 · JDK"
if [ ! -x /usr/lib/jvm/java-21-openjdk-amd64/bin/javac ]; then
  sudo apt-get update -qq && sudo apt-get install -y -qq openjdk-21-jdk-headless
fi
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version 2>&1 | head -1

echo "###STAGE 2 · swap"
if ! sudo swapon --show=NAME 2>/dev/null | grep -q /swapfile; then
  if [ -f /swapfile ]; then sudo swapon /swapfile 2>/dev/null || true; else
    sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile >/dev/null && sudo swapon /swapfile
  fi
fi

echo "###STAGE 3 · Gradle"
if [ ! -x "$GRADLE/bin/gradle" ]; then
  curl -sSfL -o /opt/tc/gradle.zip https://services.gradle.org/distributions/gradle-9.6.0-bin.zip
  ( cd /opt/tc && unzip -q -o gradle.zip && mv -f gradle-9.6.0 "$GRADLE" )
fi
"$GRADLE/bin/gradle" --version | head -3

echo "###STAGE 4 · Android SDK"
if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  curl -sSfL -o /opt/tc/clt.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  mkdir -p "$SDK/cmdline-tools"
  ( cd "$SDK/cmdline-tools" && unzip -q -o /opt/tc/clt.zip && mv -f cmdline-tools latest )
fi
SDKM="$SDK/cmdline-tools/latest/bin/sdkmanager"
yes | "$SDKM" --licenses >/dev/null 2>&1 || true
"$SDKM" "platform-tools" "platforms;android-35" "platforms;android-37.0" "build-tools;35.0.0" "build-tools;37.0.0" 2>&1 | tail -1

echo "###STAGE 5 · :app:testDebugUnitTest (StartupSmokeTest)"
cd "$REPO"
"$GRADLE/bin/gradle" :app:testDebugUnitTest --tests '*SmokeTest*' --tests '*ImageUrlTest*' --continue \
  --no-daemon --console=plain --no-watch-fs \
  --project-cache-dir=/opt/gradle-project-cache \
  -Dorg.gradle.jvmargs="-Xmx2560m -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8" \
  -Dorg.gradle.workers.max=1 -Dorg.gradle.parallel=false \
  -Dkotlin.compiler.execution.strategy=in-process \
  2>&1 | tee /home/user/test-run.log || true

echo "###REPORT"
ls -la "$REPO/app/build/test-results/testDebugUnitTest/" 2>/dev/null || true
for f in "$REPO"/app/build/test-results/testDebugUnitTest/*.xml; do
  echo "--- $f"; sed -n '1,200p' "$f" | head -120
done
echo "###DONE"
