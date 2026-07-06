# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve Epson SDK
-keep class com.epson.epos2.** { *; }
-keep interface com.epson.epos2.** { *; }
-keep class com.epson.eposprint.** { *; }

# Preserve Hoin SDK
-keep class com.example.hoinprinterlib.** { *; }
-keep class com.example.hoinsdk.** { *; }
-dontwarn am.util.printer.**

# Preserve Volley
-keep class com.android.volley.** { *; }

# Preserve native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve model classes (for serialization/JSON)
-keep class edu.amrita.amritacafe.model.** { *; }
-keep class edu.amrita.amritacafe.menu.** { *; }

# Preserve R classes (sometimes needed for reflection)
-keep class **.R$* {
    <fields>;
}

# General optimizations
-dontobfuscate
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Preserve Kotlin metadata and Coroutines signatures
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlinx.coroutines.** { *; }
