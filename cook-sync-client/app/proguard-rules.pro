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
-keep public class * extends androidx.work.ListenableWorker {
    public <init>(...);
}
-keep class androidx.work.WorkerParameters

# InputMerger.fromClassName() instantiates the configured merger (built-in
# androidx.work.OverwritingInputMerger by default) via Class.forName(...).newInstance().
# WorkManager's own bundled consumer rule only does "-keep class * extends InputMerger"
# with no member spec, which keeps the class but lets R8 strip its now-unreferenced
# zero-arg constructor, so newInstance() fails with "has no zero argument constructor" on
# release builds. Keep the constructor explicitly, same as ListenableWorker above.
-keep public class * extends androidx.work.InputMerger {
    public <init>(...);
}

# Cloudinary's core SDK resolves its network layer purely via
# Class.forName("com.cloudinary.android.UploaderStrategy").newInstance() (see
# StrategyLoader), trying a hardcoded list of platform-specific class names with no
# static reference anywhere in the app. R8 can't see that reflective use and strips/
# renames the class (or its constructor), so every candidate lookup fails on release
# builds with "UnknownError: Can't find Cloudinary platform adapter [...]" the first
# time an image is uploaded (e.g. changing the avatar).
-keep class com.cloudinary.** { *; }
-dontwarn com.cloudinary.**
