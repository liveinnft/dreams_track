# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Room classes
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keep @androidx.room.Database abstract class *
-dontwarn androidx.room.**

# Keep Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep model classes for Gson serialization
-keep class com.lionido.dreams_track.model.** { *; }
-keep class com.lionido.dreams_track.database.** { *; }

# Keep speech recognition classes
-keep class android.speech.** { *; }
-dontwarn android.speech.**

# Keep network classes (OkHttp)
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep JSON classes
-keep class org.json.** { *; }

# Don't obfuscate custom views
-keep class com.lionido.dreams_track.view.** { *; }

# Keep utils classes (important for reflection)
-keep class com.lionido.dreams_track.utils.** { *; }

# Keep activity classes
-keep class com.lionido.dreams_track.activity.** { *; }

# Keep adapter classes
-keep class com.lionido.dreams_track.adapter.** { *; }

# Keep main activity
-keep class com.lionido.dreams_track.MainActivity { *; }

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep annotation processing
-keepattributes *Annotation*
-keepclasseswithmembers class * {
    @androidx.room.* <methods>;
}

# Keep class members for reflection
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# Suppress warnings for missing classes
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn kotlin.**
-dontwarn kotlinx.**