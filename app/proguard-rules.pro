# ─────────────────────────────────────────────────────────────────────────────
#  PinBom · R8 rules for the release build
#  Release runs with isMinifyEnabled + isShrinkResources. Everything below is
#  either a class that something instantiates *by name* (the platform, the
#  WorkManager database, our own manifest) or metadata that has to survive
#  verbatim for serialization and reflection to keep working. Hilt, Room,
#  WorkManager, Retrofit, OkHttp and Coil all ship their own consumer rules;
#  these are the app-side rules layered on top of them.
# ─────────────────────────────────────────────────────────────────────────────

# Attributes the runtime reads back: annotations (@SerialName, @GET, @HiltWorker),
# generic signatures (Retrofit return types), inner/nested classes, and the line
# numbers we still want in crash reports.
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# ── kotlinx.serialization ────────────────────────────────────────────────────
# The plugin generates a serializer per @Serializable class and looks it up
# through the companion object; both must survive by name.
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation class **$$serializer { *; }
-keepclassmembers class ir.pinbom.app.** { *** Companion; }
-keepclasseswithmembers class ir.pinbom.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── DTOs and the Retrofit API ────────────────────────────────────────────────
# Request/response models are decoded by field name and the Retrofit interface is
# turned into a dynamic proxy from method annotations: keep both untouched.
-keep class ir.pinbom.app.data.remote.** { *; }
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# ── WorkManager ──────────────────────────────────────────────────────────────
# WorkSpec stores the worker class *name* in its database and instantiates it
# reflectively (through HiltWorkerFactory here), so the name must not move.
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
-keep class ir.pinbom.app.work.** { *; }

# ── Room ─────────────────────────────────────────────────────────────────────
# Entities, DAO interfaces and the database class are referenced from the
# generated _Impl; keep the package so obfuscation can never split them.
-keep class ir.pinbom.app.data.local.** { *; }

# ── Hilt ─────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# ── Platform noise from OkHttp/Okio on Android ───────────────────────────────
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
