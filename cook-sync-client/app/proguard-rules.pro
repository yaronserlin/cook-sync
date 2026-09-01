# Keep DTOs intact for Gson reflection-based (de)serialization.
-keep class com.dtos.** { *; }

# Cloudinary pulls in androidx.work (WorkManager) transitively for background uploads.
# WorkManager auto-initializes via a manifest ContentProvider and builds a Room database
# (WorkDatabase) whose generated *_Impl classes are instantiated purely via reflection,
# so R8 can't see the reference and strips them, crashing with
# "Failed to create an instance of androidx.work.impl.WorkDatabase" on release-only launch.
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.work.impl.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keep public class * extends androidx.work.ListenableWorker {
    public <init>(...);
}
-keep class androidx.work.WorkerParameters
