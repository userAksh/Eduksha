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

# Keep the generated $$serializer classes themselves — the INSTANCE field above is
# not enough; R8 also eliminates the class body which breaks runtime class lookup.
-keep,includedescriptorclasses class com.dobi.app.**$$serializer { *; }
-keepclasseswithmembers class com.dobi.app.** {
    kotlinx.serialization.KSerializer serializer(...);
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

# Top-level @Serializable types in the navigation package (e.g. ConceptListMode)
# are looked up by their fully-qualified class name at route deserialization
# time. R8 was renaming them, so AndroidX Nav's serializer threw
# "Cannot find class with name …" when navigating. Keep every type in this
# package plus enum entry points.
-keep class com.dobi.app.core.navigation.** { *; }
-keepclassmembers enum com.dobi.app.core.navigation.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Firebase already ships consumer-rules; these belt-and-braces lines
# keep model classes referenced by Firestore + Crashlytics from being
# stripped by aggressive optimization passes.
-keepnames class com.google.firebase.** { *; }

# setAppVerificationDisabledForTesting is a testing API not covered by Firebase's
# own consumer rules. R8 dead-code-eliminates it in release builds without this.
-keep class com.google.firebase.auth.FirebaseAuthSettings {
    public void setAppVerificationDisabledForTesting(boolean);
}

# ── Hilt generated code is already kept by the plugin's consumer rules.
# (Listed here as a reminder; no extra directive required.)
