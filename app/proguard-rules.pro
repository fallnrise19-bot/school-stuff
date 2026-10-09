# Existing installations store these models as Gson JSON with the original field
# names. Keep only the persisted models, not whole packages or Android libraries.
# Keeping the classes/fields also lets Gson construct them reflectively in R8 full mode.
-keep,allowoptimization class ca.creativepixels.schoolstuff.data.ChildProfile { <fields>; <init>(...); }
-keep,allowoptimization class ca.creativepixels.schoolstuff.data.SchoolItem { <fields>; <init>(...); }
-keep,allowoptimization class ca.creativepixels.schoolstuff.data.SchoolAbsence { <fields>; <init>(...); }
-keep,allowoptimization class ca.creativepixels.schoolstuff.data.SchoolDocument { <fields>; <init>(...); }
-keep,allowoptimization class ca.creativepixels.schoolstuff.data.TransportationInfo { <fields>; <init>(...); }
-keep,allowoptimization class ca.creativepixels.schoolstuff.data.TeacherContact { <fields>; <init>(...); }
-keep,allowoptimization class ca.creativepixels.schoolstuff.billing.CachedReceipt { <fields>; <init>(...); }

# Already-scheduled WorkManager jobs store this class's name in their database.
# Preserve it across the first optimized update so reminders can still run.
-keep,allowoptimization class ca.creativepixels.schoolstuff.notifications.ReminderWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
