package ca.creativepixels.schoolstuff

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) CrashDiagnostics.install(this)

        val navigator = SchoolStuffNavigator()
        val internalNavigationBackCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                navigator.navigateBack()
            }
        }
        navigator.setBackAvailabilityListener { canNavigateBack ->
            internalNavigationBackCallback.isEnabled = canNavigateBack
        }
        onBackPressedDispatcher.addCallback(this, internalNavigationBackCallback)

        setContent {
            val vm: SchoolStuffViewModel = viewModel()
            SchoolStuffApp(vm, navigator)
        }

        if (BuildConfig.DEBUG) {
            window.decorView.post { CrashDiagnostics.showPending(this) }
        }
    }
}
