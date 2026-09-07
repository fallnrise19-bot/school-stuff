package ca.creativepixels.schoolstuff

import android.content.Context
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
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

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Paper.toArgb()
        window.navigationBarColor = SoftBlue.toArgb()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
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
                    pendingAuthentication?.invoke(true, tr("Unlocked.", "Déverrouillée."))
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
            Toast.makeText(this, tr("App lock was turned off because phone security is no longer set up.", "Le verrouillage de l’appli a été désactivé, car la sécurité du téléphone n’est plus configurée."), Toast.LENGTH_LONG).show()
        }

        updateRecentsPrivacy()
        setContent {
            if (appUnlocked) {
                val vm: SchoolStuffViewModel = viewModel()
                SchoolStuffApp(
                    vm = vm,
                    appLockEnabled = appLockEnabled,
                    onChangeAppLock = ::changeAppLock,
                    onTestAppLock = ::testAppLock,
                    appLanguage = store.getAppLanguage(),
                    onChangeLanguage = ::changeLanguage
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
                            Text(tr("ParentBell is locked", "ParentBell est verrouillée"), color = Ink, fontWeight = FontWeight.Black, fontSize = 24.sp)
                            Text(tr("Use your fingerprint, face, or phone screen lock to continue.", "Utilisez votre empreinte, votre visage ou le verrouillage de l’écran pour continuer."), color = Ink.copy(alpha = .65f))
                            if (lockMessage.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(lockMessage, color = SchoolRed, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(18.dp))
                            Button(onClick = ::requestUnlock) { Text(tr("Unlock", "Déverrouiller")) }
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
        authenticate(tr("Unlock ParentBell", "Déverrouiller ParentBell")) { success, message ->
            if (success) {
                appUnlocked = true
                lockMessage = ""
            } else {
                lockMessage = message
            }
        }
    }

    private fun changeAppLock(enable: Boolean, result: (Boolean, String) -> Unit) {
        authenticate(if (enable) tr("Turn on ParentBell lock", "Activer le verrouillage de ParentBell") else tr("Turn off ParentBell lock", "Désactiver le verrouillage de ParentBell")) { success, message ->
            if (success) {
                store.setAppLockEnabled(enable)
                appLockEnabled = enable
                appUnlocked = true
                updateRecentsPrivacy()
                result(true, if (enable) tr("App lock is on.", "Le verrouillage est activé.") else tr("App lock is off.", "Le verrouillage est désactivé."))
            } else {
                result(false, message)
            }
        }
    }

    private fun testAppLock(result: (Boolean, String) -> Unit) {
        authenticate(tr("Test ParentBell unlock", "Tester le déverrouillage de ParentBell")) { success, message ->
            result(success, if (success) tr("Unlock test worked.", "Le test de déverrouillage a réussi.") else message)
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) tr("Use fingerprint, face, PIN, pattern, or password", "Utilisez votre empreinte, votre visage, votre NIP, votre schéma ou votre mot de passe")
                    else tr("Use fingerprint or face", "Utilisez votre empreinte ou votre visage")
                )
                .setAllowedAuthenticators(authenticators)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            promptBuilder.setNegativeButtonText(tr("Cancel", "Annuler"))
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
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> tr("Set up a fingerprint, face, or phone screen lock in Samsung Settings first.", "Configurez d’abord une empreinte, un visage ou un verrouillage d’écran dans les paramètres Samsung.")
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> tr("This phone does not provide biometric security.", "Ce téléphone ne prend pas en charge la sécurité biométrique.")
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> tr("Phone security is temporarily unavailable. Please try again.", "La sécurité du téléphone est temporairement indisponible. Réessayez.")
        else -> tr("Phone security is not available right now.", "La sécurité du téléphone n’est pas disponible pour le moment.")
    }

    private fun changeLanguage(language: String) {
        if (language != store.getAppLanguage()) {
            store.setAppLanguage(language)
            recreate()
        }
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
