package ca.creativepixels.schoolstuff

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.creativepixels.schoolstuff.data.LocalStore

class MainActivity : FragmentActivity() {
    private lateinit var store: LocalStore
    private lateinit var biometricPrompt: BiometricPrompt
    private var appLockEnabled by mutableStateOf(false)
    private var appUnlocked by mutableStateOf(true)
    private var lockMessage by mutableStateOf("")
    private var promptWhenResumed = false
    private var authenticationInProgress = false
    private var lastStoppedAt: Long? = null
    private var pendingAuthentication: ((Boolean, String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LocalStore(this)
        appLockEnabled = store.isAppLockEnabled()
        appUnlocked = !appLockEnabled
        promptWhenResumed = appLockEnabled

        biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authenticationInProgress = false
                    pendingAuthentication?.invoke(true, "Unlocked.")
                    pendingAuthentication = null
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authenticationInProgress = false
                    pendingAuthentication?.invoke(false, errString.toString())
                    pendingAuthentication = null
                }
            }
        )

        if (appLockEnabled && shouldDisableUnavailableLock()) {
            appLockEnabled = false
            appUnlocked = true
            promptWhenResumed = false
            store.setAppLockEnabled(false)
            Toast.makeText(this, "App lock was turned off because phone security is no longer set up.", Toast.LENGTH_LONG).show()
        }

        updateRecentsPrivacy()
        setContent {
            if (appUnlocked) {
                val vm: SchoolStuffViewModel = viewModel()
                SchoolStuffApp(
                    vm = vm,
                    appLockEnabled = appLockEnabled,
                    onChangeAppLock = ::changeAppLock,
                    onTestAppLock = ::testAppLock
                )
            } else {
                SchoolStuffTheme {
                    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(28.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = SchoolBlue)
                            Spacer(Modifier.height(14.dp))
                            Text("School Stuff is locked", color = Ink, fontWeight = FontWeight.Black, fontSize = 24.sp)
                            Text("Use your fingerprint, face, or phone screen lock to continue.", color = Ink.copy(alpha = .65f))
                            if (lockMessage.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(lockMessage, color = SchoolRed, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(18.dp))
                            Button(onClick = ::requestUnlock) { Text("Unlock") }
                        }
                    }
                }
            }
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (promptWhenResumed && appLockEnabled && !appUnlocked) {
            promptWhenResumed = false
            requestUnlock()
        }
    }

    override fun onStart() {
        super.onStart()
        val stoppedAt = lastStoppedAt
        if (appLockEnabled && stoppedAt != null && SystemClock.elapsedRealtime() - stoppedAt >= LOCK_AFTER_MILLIS) {
            appUnlocked = false
            promptWhenResumed = true
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            lastStoppedAt = SystemClock.elapsedRealtime()
        }
    }

    private fun requestUnlock() {
        authenticate("Unlock School Stuff") { success, message ->
            if (success) {
                appUnlocked = true
                lockMessage = ""
            } else {
                lockMessage = message
            }
        }
    }

    private fun changeAppLock(enable: Boolean, result: (Boolean, String) -> Unit) {
        authenticate(if (enable) "Turn on School Stuff lock" else "Turn off School Stuff lock") { success, message ->
            if (success) {
                store.setAppLockEnabled(enable)
                appLockEnabled = enable
                appUnlocked = true
                updateRecentsPrivacy()
                result(true, if (enable) "App lock is on." else "App lock is off.")
            } else {
                result(false, message)
            }
        }
    }

    private fun testAppLock(result: (Boolean, String) -> Unit) {
        authenticate("Test School Stuff unlock") { success, message ->
            result(success, if (success) "Unlock test worked." else message)
        }
    }

    private fun authenticate(title: String, result: (Boolean, String) -> Unit) {
        if (authenticationInProgress) return
        val authenticators = allowedAuthenticators()
        val availability = BiometricManager.from(this).canAuthenticate(authenticators)
        if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
            result(false, availabilityMessage(availability))
            return
        }
        authenticationInProgress = true
        pendingAuthentication = result
        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "Use fingerprint, face, PIN, pattern, or password"
                    else "Use fingerprint or face"
                )
                .setAllowedAuthenticators(authenticators)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            promptBuilder.setNegativeButtonText("Cancel")
        }
        biometricPrompt.authenticate(promptBuilder.build())
    }

    private fun shouldDisableUnavailableLock(): Boolean {
        return when (BiometricManager.from(this).canAuthenticate(allowedAuthenticators())) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> true
            else -> false
        }
    }

    private fun availabilityMessage(code: Int): String = when (code) {
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Set up a fingerprint, face, or phone screen lock in Samsung Settings first."
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "This phone does not provide biometric security."
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Phone security is temporarily unavailable. Please try again."
        else -> "Phone security is not available right now."
    }

    private fun updateRecentsPrivacy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(!appLockEnabled)
        }
    }

    private fun allowedAuthenticators(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else {
        BiometricManager.Authenticators.BIOMETRIC_WEAK
    }

    companion object {
        private const val LOCK_AFTER_MILLIS = 30_000L
    }
}
