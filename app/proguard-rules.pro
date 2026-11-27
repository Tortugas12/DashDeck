# Keep kotlinx.serialization generated serializers and Companion objects
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.dashdeck.app.**$$serializer { *; }
-keepclassmembers class com.dashdeck.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.dashdeck.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room 3.0 database implementation
-keep class * extends androidx.room3.RoomDatabase
-dontwarn androidx.room3.paging.**
