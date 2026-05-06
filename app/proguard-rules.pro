# Add project specific ProGuard rules here.
# See http://developer.android.com/guide/developing/tools/proguard.html

# Keep stack traces readable in Play Console crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Firestore data classes ────────────────────────────────────────────────
# We deserialize via `doc.toObject(SomeClass::class.java)` and
# `data["field"] as ...` casts. R8 doesn't see those reflective entry
# points, so every domain model must be kept verbatim along with its
# no-arg constructor and field names.
-keep class com.dobi.app.domain.model.** { *; }
-keepclassmembers class com.dobi.app.domain.model.** {
    <init>();
    <init>(...);
}

# Enums in domain.model are looked up by name (e.g. ParentTaskStatus.valueOf,
# RevisionScope.fromKey reads the .name property). Keep their static methods.
-keepclassmembers enum com.dobi.app.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── kotlinx.serialization (type-safe nav routes via @Serializable) ────────
-keepattributes *Annotation*, InnerClasses

-keepclassmembers @kotlinx.serialization.Serializable class * {
    public static **$Companion Companion;
    public static **$$serializer INSTANCE;
}

-keep class com.dobi.app.core.navigation.Screen { *; }
-keep class com.dobi.app.core.navigation.Screen$* { *; }
-keepclassmembers class com.dobi.app.core.navigation.Screen$* {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

-if class com.dobi.app.core.navigation.Screen$* {
    static **$Companion Companion;
}
-keep class com.dobi.app.core.navigation.Screen$*$$serializer { *; }

# ── Firebase already ships consumer-rules; these belt-and-braces lines
# keep model classes referenced by Firestore + Crashlytics from being
# stripped by aggressive optimization passes.
-keepnames class com.google.firebase.** { *; }

# ── Hilt generated code is already kept by the plugin's consumer rules.
# (Listed here as a reminder; no extra directive required.)
