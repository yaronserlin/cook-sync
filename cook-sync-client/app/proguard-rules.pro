# Keep DTOs intact for Gson reflection-based (de)serialization.
-keep class com.dtos.** { *; }

# RecipeDraft (and its nested DraftIngredient/DraftInstruction rows) is independently
# (de)serialized to/from JSON by RecipeDraftStore for local "Save draft"/resume, exactly like
# the com.dtos.** DTOs above — but unlike them, it isn't part of any kept API surface, so R8's
# optimizer is free to narrow its List<DescriptionBlockDTO> field down to a raw ArrayList and
# drop the field entirely from the reflection-visible surface. That strips exactly the generic
# signature Gson needs (see the -keepattributes Signature comment below), breaking
# RecipeDraftStore.loadAll()'s deserialization the same way — a resumed/saved draft's
# description blocks come back as LinkedTreeMap instead of DescriptionBlockDTO.
-keep class com.cooksync.app.data.model.recipe.RecipeDraft { *; }
-keep class com.cooksync.app.data.model.recipe.RecipeDraft$* { *; }

# R8 strips generic signature metadata by default. Gson relies on Field.getGenericType() to
# resolve a field's element type for any generic collection (e.g. List<DescriptionBlockDTO>);
# without this, it can't tell the field apart from a raw List and deserializes each JSON object
# as a com.google.gson.internal.LinkedTreeMap instead, which throws a ClassCastException the
# first time that element is later read as its real DTO type (e.g.
# WizardDescriptionBlockAdapter#onBindViewHolder) — release-build only, since debug builds skip
# R8 entirely. *Annotation* additionally keeps @SerializedName usable by Gson's reflection.
-keepattributes Signature
-keepattributes *Annotation*

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
