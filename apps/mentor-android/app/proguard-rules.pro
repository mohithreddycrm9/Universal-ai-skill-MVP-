-keepattributes *Annotation*
-keepclassmembers class * {
    @com.squareup.moshi.* <methods>;
}

-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class kotlinx.coroutines.flow.**

-keep class com.skillmcp.mentor.data.db.** { *; }
-keep class com.skillmcp.mentor.llm.LlmProviderKind { *; }

-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.** { *; }
