# Coffer R8 rules (S23-H03). Room, Hilt, Retrofit/OkHttp and Firebase ship their own
# consumer rules; kotlinx.serialization also ships rules, but we keep the official ones
# explicit because serializer stripping under R8 full mode is the one real risk here.

# --- kotlinx.serialization (official rules) ---
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Keep the Companion of every @Serializable class and its serializer().
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializer() on @Serializable objects (INSTANCE).
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
