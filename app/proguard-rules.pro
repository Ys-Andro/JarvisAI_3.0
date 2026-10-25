# Project ProGuard Rules for Jarvis AI

# Preserve line numbers and source file attributes for stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Room Database
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# Firebase & Firestore Models
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.DocumentId <methods>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.ServerTimestamp <methods>;
}
-keep class com.example.jarvisai.data.model.todo.** { *; }
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# org.json
-keep class org.json.** { *; }
-dontwarn org.json.**

# Manifest components (Services, Receivers, Providers)
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep class com.example.jarvisai.data.service.** { *; }
-keep class com.example.jarvisai.data.util.JarvisAccessibilityService { *; }
-keep class com.example.jarvisai.data.util.JarvisNotificationListenerService { *; }
-keep class com.example.jarvisai.presentation.widget.JarvisAppWidgetProvider { *; }
-keep class com.example.jarvisai.data.automation.AutomationApprovalReceiver { *; }

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
