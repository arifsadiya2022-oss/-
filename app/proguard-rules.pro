# R8 / ProGuard rules for ARIF Game Booster

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep BuildConfig metadata
-keep class com.example.BuildConfig { *; }

# Keep Foreground Services & Activity entry points referenced by AndroidManifest
-keep class com.example.MainActivity { *; }
-keep class com.example.service.VoiceChangerService { *; }
-keep class com.example.service.FloatingSidebarService { *; }
-keep class com.example.service.OverlayLifecycleOwner { *; }

# Keep ViewModels instantiated via ViewModelProvider
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}
