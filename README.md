# PinBom Android · پینبوم

اپ اندروید PinBom — Kotlin + Jetpack Compose، Material 3، دوزبانه (فارسی/انگلیسی)،
با احراز هویت، فید، جستجو، ساخت پین، بورد، اعلان، آفلاین (Room) و آپلود پس‌زمینه (WorkManager).

نسخهٔ فعلی: **1.0.0** · بیلد **#11** (نمایش تصویرها: `image_url` سرور + یک هلپر آدرس‌سازی در کلاینت) · `ir.pinbom.app`

---

## باز کردن پروژه

Android Studio → **Open** → پوشهٔ `pinbom-android` (همین پوشه) → صبر کنید تا Gradle Sync تمام شود.
هیچ کار خاصی لازم نیست؛ `local.properties` را خودِ Android Studio با مسیر SDK شما می‌سازد.

**پیش‌نیازها**

| | |
|---|---|
| JDK | 17 یا 21 (JVM target پروژه: 17) |
| Android Studio | نسخه‌ای که AGP 9.4 را پشتیبانی کند (Ladybug+ / جدیدتر) |
| Gradle | 9.6.0 — داخل `gradlew` هست، لازم نیست جدا نصب کنید |
| Android SDK | `compileSdk 37` (platform `android-37.0`)، `targetSdk 35`، `minSdk 24` |
| Build-Tools | `35.0.0` و `37.0.0` |

## ساخت APK

**Linux / macOS**

```bash
./gradlew :app:assembleDebug
# خروجی: app/build/outputs/apk/debug/app-debug.apk
```

**Windows**

```powershell
.\gradlew.bat :app:assembleDebug
```

**اجرای تست‌ها (گارد راه‌اندازی + بررسی منابع)**

```bash
./gradlew :app:testDebugUnitTest
```

> در سندباکس لینوکسی بدون Android Studio، اسکریپت‌های آماده در `tools/` هستند:
> `tools/build-apk.sh` (نصب زنجیره‌ابزار + `assembleDebug`)،
> `tools/run-tests.sh` (تست‌ها)،
> `tools/verify-and-build.sh` (اول تست، بعد بیلد).

## نصب روی گوشی

```bash
adb install -r pinbom-1.0.0-debug.apk
```

نسخهٔ `debug` با همان گواهی debug امضا شده؛ روی نسخهٔ قبلی بدون حذف داده نصب می‌شود.
برای انتشار، نسخهٔ `release` با کیستور خودتان لازم است.

## مستندات

| فایل | محتوا |
|---|---|
| `BUILD-NOTES.md` | نتیجهٔ بیلد جاری، دستورهای ساخت، و نکته‌های پیکربندی Gradle |
| `REBRAND-NOTES.md` | ریبرند Bumo → PinBom: نگاشت تغییرها، سازگاری با نصب‌های قبلی، تأییدها |
| `brand/README.md` | لوگو، آیکون‌ها و نحوهٔ بازتولیدشان |

## تصویرها از کجا می‌آیند (نمونهٔ سرتاسری)

سرور در هر پاسخ، آدرس تصویر را **آمادهٔ واکشی** می‌دهد: `image_url` یک مسیر ریشه‌دار
است، مثل `/media/1/2026/10/e78057c6291983e17c6934dd410c7e4b_medium.jpg`. کلاینت فقط
رشتهٔ `baseUrl` را به آن می‌چسباند — هیچ‌جای دیگری آدرس ساخته نمی‌شود (تابع `imageUrl`
در `ui/components/PinImage.kt` تنها نقطهٔ آدرس‌سازی است؛ `PinCard`، `Avatar` و صفحه‌ها همه
از همان استفاده می‌کنند).

گام ۱ — همان تصویری که اپ می‌بیند، با curl:

```bash
BASE=http://localhost:8080
TOKEN=$(curl -s -X POST $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"identifier":"negargar","password":"pinbom-demo-2025"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
IMG=$(curl -s $BASE/api/v1/pins/1 -H "Authorization: Bearer $TOKEN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["image_url"])')
curl -i "$BASE$IMG"          # → HTTP/1.1 200 OK , Content-Type: image/jpeg
```

گام ۲ — همان چیز در Compose، با همان اجزای واقعی پروژه (Retrofit + Hilt + Coil):

```kotlin
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ir.pinbom.app.data.PinBomRepository
import ir.pinbom.app.data.SessionManager
import ir.pinbom.app.data.remote.Pin
import ir.pinbom.app.ui.components.PinImage
import ir.pinbom.app.ui.components.imageUrl

/**
 * کمینهٔ «پین ۱ را بگیر و نشان بده». در اپ واقعی همان PinImage داخل PinCard
 * و صفحهٔ جزئیات هم به همین شکل صدا زده می‌شود.
 */
@Composable
fun PinOne(repository: PinBomRepository, session: SessionManager) {
    var pin by remember { mutableStateOf<Pin?>(null) }

    LaunchedEffect(Unit) { pin = runCatching { repository.pin(1L) }.getOrNull() }

    pin?.let { item ->
        PinImage(
            url = imageUrl(session.baseUrl, item.image_url),   // «/media/…» → «http://host:8080/media/…»
            seed = item.id,
            modifier = Modifier.fillMaxWidth(),
            aspectRatio = item.width.toFloat() / item.height.coerceAtLeast(1).toFloat(),
        )
    }
}
```

اگر تصویر بالا نیامد، تگ لاگ `PinBomImage` را نگاه کنید:

```bash
adb logcat -s PinBomImage:W PinBomNet:D
```

- `PinBomImage … failed to load http://… — <Exception>: <message>` → مشکل در واکشی
  (شبکه/آدرس/۴۰۴) که خودِ آدرس حل‌شده را هم چاپ می‌کند.
- اگر خطی چاپ نشد ولی تصویر نبود، آدرس درست رسیده و ایراد در دیکد/فرمت است.

## ساختار

```
app/src/main/java/ir/pinbom/app/
  ├── data/        Retrofit + DTO + Room + SessionManager
  ├── di/          ماژول‌های Hilt
  ├── ui/
  │   ├── components/  کامپوننت‌های مشترک (PinCard، Brand، …)
  │   ├── nav/         AppNavGraph (پوستهٔ ناوبری)
  │   ├── screens/     یک فایل برای هر صفحه
  │   └── theme/       رنگ/تایپ/جهت/دوزبانه (PinBomTheme)
  ├── work/        UploadWorker (آپلود پس‌زمینه)
  └── ThemeStore.kt  انتخاب تم و زبان (DataStore)
app/src/test/      تست‌های JVM (گارد راه‌اندازی، منابع)
```
