# AndroidJUnitRunner uses tracing from the tested APK's dependency graph. It is
# otherwise unused in the app and R8 removes it before packaging instrumentation.
# This rule is exclusive to optimizedTest, never applied to the Play release.
-keep class androidx.tracing.Trace { *; }

# The instrumentation runner also loads Kotlin runtime entry points dynamically.
# Keep the shared runtime for the test harness; app classes still undergo R8.
# This is test-only and does not affect release size or optimization metrics.
-keep class kotlin.** { *; }
