# ProGuard / R8 Rules for TOEIC Flashcard (Release)

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Seed & custom JSON DTOs
-keep class com.toeicflashcard.data.model.** { *; }

# Keep all kotlinx.serialization models, Companions, and generated serializers
# (Protects SeedVocabularyDto, AddCustomVocabularyUseCase DTO, and Navigation type-safe Routes)
-keep @kotlinx.serialization.Serializable class com.toeicflashcard.** { *; }
-keepclassmembers class com.toeicflashcard.** {
    static ** Companion;
    *** serializer(...);
}
-keep,includedescriptorclasses class com.toeicflashcard.**$$serializer { *; }

# Room entities, DAOs, and Database
-keep class com.toeicflashcard.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# WorkManager daily reminder worker
-keep class com.toeicflashcard.data.worker.DailyReminderWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Preserve enum values()/valueOf() used in Compose state & serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Haze blur library
-keep class dev.chrisbanes.haze.** { *; }
