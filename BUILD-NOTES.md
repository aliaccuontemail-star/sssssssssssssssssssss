# ساخت APK — گزارش بیلد (PinBom 1.0.0)

## بیلد جاری

| | |
|---|---|
| فایل خروجی | `/home/user/pinbom-1.0.0-debug.apk` |
| حجم | ‏24,035,240 بایت |
| MD5 | `cf10eff866dd8eb882132c1d1c716930` |
| بسته | `ir.pinbom.app` — versionCode 1، versionName 1.0.0 |
| امضا | **debug** (گواهی Android Debug) — نصب‌شدنی، ولی برای انتشار در Play باید با کلید release امضا شود |
| targetSdk / minSdk | ‏35 / 24 (compileSdk = 37) |
| Locales | `en` و `fa` — برچسب‌ها: `PinBom` / `پینبوم` |
| بستهٔ سورس | `/home/user/pinbom-android-1.0.0.zip` |

نصب سریع روی دستگاه متصل:

```bash
adb install -r /home/user/pinbom-1.0.0-debug.apk
```

سپس در اولین اجرا آدرس سرور را در صفحهٔ «اتصال به سرور» وارد کنید
(پیش‌فرض بیلد: `http://127.0.0.1:8080/`؛ cleartext در `network_security_config` مجاز است).

## بازتولید

```bash
bash /home/user/persian-pintrest/tools/build-apk.sh   # JDK ← swap ← Gradle 9.6.0 ← SDK ← assembleDebug ← کپی APK
```

یا دستی:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/opt/tc/sdk
./gradlew :app:assembleDebug        # APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # تست‌ها → app/build/reports/tests/
```

## نکته‌های پیکربندی بیلد (چرا پروژه این‌طور تنظیم شده)

- **`android.builtInKotlin=false` و `android.newDsl=false`** در `gradle.properties`:
  ‏AGP 9 کاتلین را درون خود دارد و افزونهٔ `org.jetbrains.kotlin.android` را رد می‌کند،
  اما Hilt و Room روی kapt هستند و kapt با همین گزینهٔ رسمی AGP کار می‌کند.
- **JVM target = 17** (`compileOptions` + `kotlin.compilerOptions`): ناهم‌ترازی
  `compileDebugJavaWithJavac` و `kaptGenerateStubsDebugKotlin` را رفع می‌کند.
- **`compileSdk = 37`** با `targetSdk = 35`: ۲۷ کتابخانهٔ کاتالوگ (core-ktx 1.19،
  Compose 1.12.1، coil3 3.6.3، navigation 2.10.2 …) حداقل compileSdk 37 می‌خواهند.
  SDK لازم: `platforms;android-37.0` + `build-tools;37.0.0` (اسکریپت خودش نصب می‌کند).
- برای انتشار: `build-apk.ps1` نام `gradle-9.6-bin.zip` را صدا می‌زند که ۴۰۴ می‌دهد؛
  نام درست `gradle-9.6.0-bin.zip` است (فایل دست‌نخورده مانده — فقط برای اطلاع).

## تاریخچهٔ بیلدها

| بیلد | تغییر | حجم | MD5 |
|---|---|---|---|
| #1–#6 | ساخت اولیه و پالایش فازهای ۱–۹ | — | — |
| #7 | حذف OTP · لوگوی جدید · دوزبانهٔ fa/en | ‏23,815,025 | `27c1efd81ed7bb2acdf8b17a6cfe9ed6` |
| #8 | اصلاح کرشِ باز شدن (`LocalContext` در `PinBomTheme`) | ‏23,815,025 | `2c982e741b5ad4422f66fb52284aa7a8` |
| **#9** | **ریبرند Bumo → PinBom / پینبوم** | **‏23,966,099** | **`e22116ccb01410bae689e3ce09bb25a6`** |

بیلد #9: `assembleDebug` در **10m 21s** و `testDebugUnitTest` در **2m 32s** — **۱۴ تست، ۰ شکست**
(`FirstRunSmokeTest`, `StartupSmokeTest` ×۵، `ResourcesSmokeTestApi24/35` ×۳+۳، `ImageUrlTest` ×۲).
بازرسی APK: `package=ir.pinbom.app` · `application-label:'PinBom'` · `application-label-fa:'پینبوم'` ·
اسکیم‌های `pinbom` و `bumo` هر دو فعال. جزئیات ریبرند: `REBRAND-NOTES.md`.


---

## بیلد #10 — رفع «آپلود بی‌صدا می‌میرد» و «درخواست به آدرس سرور نمی‌رود»

### ۱) نمونه‌سازی UploadWorker با Hilt

**علت:** کلاس `UploadWorker` یک `@HiltWorker` با پارامترهای تزریقی است، پس با سازندهٔ
دو‌آرگومانی `WorkManager` ساخته نمی‌شود. کاتالوگ و `kapt` پروژه فقط
`com.google.dagger:hilt-compiler` را داشتند و پردازشگر `androidx.hilt:hilt-compiler`
(مسئول تولید نقشهٔ `HiltWorkerFactory`) غایب بود؛ در نتیجه `HiltWorkerFactory`
کارگر را پیدا نمی‌کرد و `NoSuchMethodException` می‌داد.

**تغییرها**
- `gradle/libs.versions.toml`: `androidx-hilt-compiler = { module = "androidx.hilt:hilt-compiler", version.ref = "hiltX" }`
- `app/build.gradle.kts`: `kapt(libs.androidx.hilt.compiler)`
- `app/src/main/AndroidManifest.xml`: حذف `androidx.work.WorkManagerInitializer` از
  `androidx.startup.InitializationProvider` با `tools:node="remove"` تا WorkManager
  به‌جای مسابقه با `onCreate`، هنگام نیاز و از روی `Configuration.Provider` ساخته شود.

**مدرک (نه ادعا):** پس از بیلد، این فایل‌ها توسط kapt تولید شدند —
`UploadWorker_HiltModule.java`, `UploadWorker_AssistedFactory.java`,
`UploadWorker_Factory.java`, `UploadWorker_AssistedFactory_Impl.java` — و در مانیفست
نهایی APK دیگر هیچ ردی از `WorkManagerInitializer` نیست (فقط `EmojiCompatInitializer`
باقی است). دو تست نگهبان هم اضافه شد (`UploadWorkerWiringTest`) که یکی وجود کارخانهٔ
تولیدشده و دیگری اینکه کلید نقشه دقیقاً نام کلاس کارگر است را بررسی می‌کند.

### ۲) «درخواست به آدرسی که مشخص کردیم نمی‌رود»

**علت:** صفحهٔ اتصال هر آدرس خوش‌ساخت را بی‌آزمایش ذخیره می‌کرد. آدرس پیش‌فرض فیلد
`http://127.0.0.1:8080` است، که روی *گوشی* یعنی خود گوشی (روی شبیه‌ساز هم یعنی خودِ
شبیه‌ساز). اگر کسی آن را دست‌نزده رد می‌کرد، هیچ پیامی نمی‌دید و بعداً روی صفحهٔ
ثبت‌نام یک خطای اتصال خام ظاهر می‌شد.

**تغییرها**
- `data/remote/ServerProbe.kt` (جدید): پیش از ذخیره، `GET {آدرس}/health` با مهلت ۶
  ثانیه. تا سرور جواب ندهد، آدرس ذخیره نمی‌شود و صفحه جلو نمی‌رود.
- `ui/screens/ServerSetupScreen.kt`: حالت خطا + راهنمای آدرس loopback؛ پیام‌ها در
  `values` و `values-fa`.
- `data/remote/ServerAddress.kt` (جدید) + `Network.kt`: منطق «کجا فرستاده می‌شود» از
  اینترسپتور بیرون کشیده شد (تست‌پذیر)، پورت هرگز حذف نمی‌شود و پیشوند مسیر آدرس
  سرور پشتیبانی می‌شود؛ در بیلد debug هر فراخوانی با تگ `PinBomNet` لاگ می‌شود.

**مدرک:** `ServerAddressTest` (۸ تست) — میزبان/پورت/اسکیم از سرور می‌آید، پیشوند
مسیر حفظ می‌شود، کوئری‌ها سالم می‌مانند، آدرس خالی/بی‌میزبان رد می‌شود.

### تست و بیلد

`./gradlew :app:assembleDebug :app:testDebugUnitTest` → **BUILD SUCCESSFUL**،
**۲۴ تست، ۰ شکست** (۱۴ قبلی + ۸ + ۲ جدید).

> نکتهٔ محیط: با ۲ گیگ رم، `-Xmx3g` در `gradle.properties` باعث OOM-kill شدن دیمن
> می‌شود. برای بیلد در این ماشین از
> `--max-workers=1 -Dorg.gradle.jvmargs="-Xmx1300m -XX:MaxMetaspaceSize=512m -XX:+UseSerialGC" -Dkotlin.daemon.jvmargs="-Xmx700m"`
> استفاده شد. روی ماشین شما نیازی نیست.

---

## بیلد #11 — نمایش تصویر پین‌ها، سرتاسری (سرور + اپ)

### علت

پاسخ‌های API مسیر **خامِ ذخیره‌سازی** را می‌فرستادند
(`2/2026/10/<hash>.jpg`) در حالی که فایل‌ها زیر `/media/` سرو می‌شوند. کلاینت آن را به
`baseUrl` می‌چسباند و آدرسی می‌ساخت که وجود نداشت ⇒ هر تصویر ۴۰۴. ایراد در سرور بود،
نه در Coil یا در شبکه.

### تصمیم‌ها

| پرسش | انتخاب | چرا |
|---|---|---|
| URL نسبی یا مطلق؟ | **نسبی، با پیشوند** (`/media/2/2026/10/x.jpg`) | آدرس با هر دامنه/پورتی کار می‌کند (شبیه‌ساز 10.0.2.2، IP لن، دامنهٔ واقعی) و در مرورگر هم باز می‌شود |
| پیشوند | **`/media/`** | همان چیزی که از قبل در پروژه سرو می‌شد |
| دسترسی | **عمومی/بی‌توکن** | تصویر بدون هدر قابل واکشی است ⇒ کش CDN، `<img>` و کش دیسک Coil بدون ترفند توکن کار می‌کنند؛ نام فایل‌ها hash تصادفی است و مسیر پوشه‌ها از قبل عکسِ بدون‌قصد است |
| کتابخانه | **Coil 3** (همان کتابخانهٔ موجود اپ) | کتابخانهٔ جدیدی اضافه نشد |

### تغییرهای سرور (Go)

- `internal/models/models.go` — `const MediaPrefix = "/media"` و
  `Pin.StoragePaths []string` با `json:"-"` (مسیرهای خام برای فایل‌خوانی/حذف).
- `internal/repositories/media.go` **(جدید)** — یک تابع `mediaURL()` که تنها نقطهٔ
  تبدیل مسیر به آدرس است، به‌همراه `normalizePinMedia()` و `storagePaths()`.
- `internal/repositories/pins.go` ×۲، `boards.go` ×۳، `users.go` ×۲ — همهٔ اسکنرها
  از `normalizePinMedia` رد می‌شوند؛ پس **هر** پاسخی که پین/بورد/کاربر را سریالایز
  می‌کند (`create`, فیدها، جزئیات، جستجو، پروفایل، بوردها) آدرس درست می‌دهد.
- `internal/services/pin_service.go` — حذف فایل‌ها با `StoragePaths` (نه `ImageURL`)،
  و لاگ `file` / `variants` / `url` روی ساخت پین.
- `internal/handlers/media.go` + `internal/server/server.go` — مسیرهای
  `GET/HEAD /media/*` با `Cache-Control: public, max-age=31536000, immutable`،
  `http.ServeContent` (Range/HEAD/304)، نگهبان مسیر (`..` و مسیرهای بیرون ریشه → 404)،
  تشخیص نوع با پسوند و در نبود پسوند با sniff، و **لاگ WARN** برای ۴۰۴/خطای خواندن
  همراه مسیر درخواست و `request_id`.
- تست‌ها/اسکریپت‌ها: `internal/server/server_test.go`،
  `internal/repositories/integration_test.go`، `scripts/smoke.sh` و `README.md`
  سرور هم‌راستا شدند (تأیید پیشوند `/media/` در `image_url`).

### تغییرهای اپ (Android)

- `ui/components/PinImage.kt` — `imageUrl(base, path)` بازنویسی شد: URL مطلق
  (http/https/protocol-relative) دست‌نخورده؛ مسیر ریشه‌دار همان‌طور چسبانده می‌شود؛
  مسیر خام پیشوند `/media/` می‌گیرد؛ `media/…` دوباره پیشوند نمی‌گیرد؛ اسلش تکراری/کم
  ساخته نمی‌شود. `onError` حالا آدرس حل‌شده، نوع خطا و پیام آن را با تگ
  `PinBomImage` لاگ می‌کند.
- `ui/components/Avatar.kt` — همان `imageUrl` (یک هلپر، دو مصرف‌کننده).
- `app/src/main/res/xml/network_security_config.xml` — cleartext فقط برای
  `10.0.2.2`, `localhost`, `127.0.0.1`؛ `app/src/debug/res/xml/...` — برای بیلد debug
  cleartext باز است (سرور توسعه‌دهنده روی هر IP‌ای). `android:usesCleartextTraffic`
  از مانیفست **حذف** شد؛ `android:networkSecurityConfig` سرجایش است.
- `app/src/test/java/ir/pinbom/app/ImageUrlTest.kt` — از ۲ به **۸ تست** رسید
  (مسیر روت‌شده، مسیر خام، اسلش انتهایی، مطلق http/https، عدم پیشوند تکراری،
  رشتهٔ خالی/فضا، base خالی، 10.0.2.2 و IP لن).
- `README.md` — بخش «تصویرها از کجا می‌آیند» با نمونهٔ سرتاسری (curl + Compose) برای پین ۱.

### نتیجهٔ بیلد و تست

`./gradlew :app:assembleDebug :app:testDebugUnitTest` → **BUILD SUCCESSFUL** —
**۳۰ تست، ۰ شکست** (۲۴ + ۶ تست تازهٔ ImageUrlTest).
