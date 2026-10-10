$ErrorActionPreference = "Stop"
if (-not (Test-Path ".\gradlew.bat")) { throw "Gradle wrapper is missing. Open the project once in Android Studio and generate the wrapper, or install Gradle 9.6+ and place it on PATH." }
if (-not (Test-Path ".\pinbom.jks")) {
  keytool -genkeypair -v -keystore pinbom.jks -keyalg RSA -keysize 2048 -validity 10000 -alias pinbom -storepass pinbom123 -keypass pinbom123 -dname "CN=PinBom" -noprompt
}
.\gradlew.bat assembleRelease
New-Item -ItemType Directory -Force -Path .\output | Out-Null
Copy-Item .\app\build\outputs\apk\release\app-release.apk .\output\pinbom.apk -Force
Write-Host "Built: pinbom-android/output/pinbom.apk"
