# AndroidJUnitRunner uses tracing from the tested APK's dependency graph. It is
# otherwise unused in the app and R8 removes it before packaging instrumentation.
# This rule is exclusive to optimizedTest, never applied to the Play release.
-keep class androidx.tracing.Trace { *; }
