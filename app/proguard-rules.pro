# kotlinx.serialization: keep serializers of navigation routes and DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> { static <1>$Companion Companion; }
-if @kotlinx.serialization.Serializable class ** { static **$* *; }
-keepclassmembers class <2>$<3> { kotlinx.serialization.KSerializer serializer(...); }
-if @kotlinx.serialization.Serializable class ** { public static ** INSTANCE; }
-keepclassmembers class <1> { public static <1> INSTANCE; kotlinx.serialization.KSerializer serializer(...); }

# Navigation Compose (type-safe routes) resolves enum arguments by their fully qualified serialName via
# Class.forName at runtime; keep the route package's names and enum members intact.
-keepnames class com.wallee.terminallinker.navigation.**
-keep enum com.wallee.terminallinker.navigation.** { *; }
