# Room — keep entity and DAO class names for reflection
-keep class com.cedd.budgettracker.data.local.** { *; }

# Hilt
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Coil
-dontwarn okhttp3.**
-dontwarn okio.**
