# Keep Room entities & generated implementation
-keep class dev.openfit.app.data.local.entity.** { *; }
-keep class dev.openfit.app.data.local.ExerciseCatalog** { *; }

# Keep kotlinx.serialization serializers
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class dev.openfit.app.**$$serializer { *; }
-keepclassmembers class dev.openfit.app.** {
    *** Companion;
}
-keepclasseswithmembers class dev.openfit.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.serialization.**
