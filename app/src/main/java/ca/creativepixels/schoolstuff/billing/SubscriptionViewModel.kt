package ca.creativepixels.schoolstuff.billing

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.creativepixels.schoolstuff.BuildConfig
import ca.creativepixels.schoolstuff.tr
import com.android.billingclient.api.*
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

data class SubscriptionState(
    val active: Boolean = false,
    val autoRenewing: Boolean = false,
    val pending: Boolean = false,
    val checked: Boolean = false,
    val connected: Boolean = false,
    val checking: Boolean = false,
    val purchasing: Boolean = false,
    val price: String? = null,
    val planAvailable: Boolean = false,
    val verificationConfigured: Boolean = false,
    val message: String = ""
) {
    val canSubscribe: Boolean get() = verificationConfigured && connected && checked &&
        planAvailable && !active && !pending && !checking && !purchasing
    val summary: String get() = when {
        active -> tr("Active", "Actif")
        pending -> tr("Payment pending", "Paiement en attente")
        checking -> tr("Checking Google Play…", "Vérification de Google Play…")
        !verificationConfigured -> tr("Coming soon · testing access is open", "Bientôt offert · accès de test ouvert")
        !checked -> tr("Status unavailable", "État indisponible")
        else -> tr("Not subscribed", "Aucun abonnement")
    }
}

private data class CachedReceipt(val json: String, val signature: String, val checkedAt: Long)

class SubscriptionViewModel(application: Application) : AndroidViewModel(application), PurchasesUpdatedListener {
    private val verifier = PurchaseSignatureVerifier(BuildConfig.BILLING_PUBLIC_KEY)
    private val receiptFile = File(application.noBackupFilesDir, "parentbell-subscription.json")
    private val gson = Gson()
    private val main = Handler(Looper.getMainLooper())
    private val acknowledging = mutableSetOf<String>()
    private var connecting = false
    private var closed = false
    private var lastCheckedAt = 0L
    private var queryingPurchases = false

    var state by mutableStateOf(SubscriptionState(verificationConfigured = verifier.isConfigured))
        private set

    private val client = BillingClient.newBuilder(application)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    init {
        restoreOfflineReceipt()
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                if (state.active && !SubscriptionPolicy.cacheIsFresh(lastCheckedAt, System.currentTimeMillis())) {
                    clearReceipt()
                    state = state.copy(active = false, autoRenewing = false, checked = false,
                        message = tr("Connect to Google Play to confirm your subscription.", "Connectez-vous à Google Play pour confirmer votre abonnement."))
                }
            }
        }
    }

    fun refresh() {
        if (closed || connecting || state.purchasing) return
        if (client.isReady) {
            state = state.copy(connected = true)
            queryPlan()
            queryPurchases()
            return
        }
        connecting = true
        state = state.copy(checking = true, message = "")
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) = onMain {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    state = state.copy(connected = true, checking = false)
                    queryPlan()
                    queryPurchases()
                } else {
                    state = state.copy(connected = false, checking = false, message = billingError(result.responseCode))
                }
            }
            override fun onBillingServiceDisconnected() = onMain {
                connecting = false
                state = state.copy(connected = false, checking = false, purchasing = false)
            }
        })
    }

    fun resume() {
        // A callback may have been lost while checkout was in the background.
        // Always reconcile ownership when the activity returns to the foreground.
        state = state.copy(purchasing = false)
        refresh()
    }

    fun restorePurchases() {
        if (state.purchasing || state.checking) return
        if (!client.isReady) {
            refresh()
        } else {
            queryPurchases(restoring = true)
            queryPlan()
        }
    }

    fun subscribe(activity: Activity) {
        if (!state.canSubscribe || activity.isFinishing || activity.isDestroyed) return
        state = state.copy(purchasing = true, message = "")
        // Recheck ownership and fetch a fresh offer immediately before checkout.
        // Never retain ProductDetails between screen visits or purchase attempts.
        queryPurchases(beforeCheckout = {
            if (state.active || state.pending) {
                state = state.copy(purchasing = false)
            } else {
                queryPlan { product, offer ->
                    if (product == null || offer == null || activity.isFinishing || activity.isDestroyed) {
                        state = state.copy(purchasing = false)
                    } else {
                        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(product).setOfferToken(offer.offerToken).build()
                        )).build()
                        val result = client.launchBillingFlow(activity, params)
                        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                            state = state.copy(purchasing = false, message = billingError(result.responseCode))
                            if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) queryPurchases()
                        }
                    }
                }
            }
        })
    }

    fun manageSubscription(activity: Activity) {
        val uri = Uri.parse("https://play.google.com/store/account/subscriptions")
            .buildUpon().appendQueryParameter("sku", SubscriptionPolicy.PRODUCT_ID)
            .appendQueryParameter("package", BuildConfig.APPLICATION_ID).build()
        if (runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, uri)) }.isFailure) {
            state = state.copy(message = tr("Open Google Play, then Payments & subscriptions > Subscriptions.", "Ouvrez Google Play, puis Paiements et abonnements > Abonnements."))
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) = onMain {
        state = state.copy(purchasing = false)
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                // Process the signed callback receipt even if the subsequent
                // ownership query loses its network connection.
                purchases?.firstOrNull { verify(it) }?.let { acceptPurchase(it) }
                queryPurchases()
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> queryPurchases()
            BillingClient.BillingResponseCode.USER_CANCELED -> state = state.copy(
                message = tr("Purchase cancelled. You were not subscribed.", "Achat annulé. Aucun abonnement n’a été activé."))
            else -> state = state.copy(message = billingError(result.responseCode))
        }
    }

    private fun queryPlan(checkout: ((ProductDetails?, ProductDetails.SubscriptionOfferDetails?) -> Unit)? = null) {
        val supported = client.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        if (supported.responseCode != BillingClient.BillingResponseCode.OK) {
            state = state.copy(planAvailable = false, price = null, message = billingError(supported.responseCode))
            checkout?.invoke(null, null)
            return
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId(SubscriptionPolicy.PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS).build()
        )).build()
        client.queryProductDetailsAsync(params) { result, details -> onMain {
            val product = details.productDetailsList.firstOrNull { it.productId == SubscriptionPolicy.PRODUCT_ID }
            val offer = product?.subscriptionOfferDetails?.firstOrNull { candidate ->
                val phases = candidate.pricingPhases.pricingPhaseList
                SubscriptionPolicy.isMonthlyPlan(candidate.basePlanId, candidate.offerId,
                    phases.map { it.billingPeriod }, phases.map { it.recurrenceMode },
                    phases.map { it.priceAmountMicros }, candidate.installmentPlanDetails != null)
            }
            if (result.responseCode == BillingClient.BillingResponseCode.OK && product != null && offer != null) {
                state = state.copy(planAvailable = true, price = offer.pricingPhases.pricingPhaseList.single().formattedPrice)
                checkout?.invoke(product, offer)
            } else {
                state = state.copy(planAvailable = false, price = null, message = if (result.responseCode == BillingClient.BillingResponseCode.OK)
                    tr("The monthly subscription is not available yet. Please try again later.", "L’abonnement mensuel n’est pas encore offert. Réessayez plus tard.")
                    else billingError(result.responseCode))
                checkout?.invoke(null, null)
            }
        } }
    }

    private fun queryPurchases(restoring: Boolean = false, beforeCheckout: (() -> Unit)? = null) {
        if (queryingPurchases) return
        queryingPurchases = true
        state = state.copy(checking = true, message = "")
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        client.queryPurchasesAsync(params) { result, purchases -> onMain {
            queryingPurchases = false
            state = state.copy(checking = false)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                state = state.copy(purchasing = false, message = billingError(result.responseCode))
                return@onMain
            }
            val relevant = purchases.filter { SubscriptionPolicy.PRODUCT_ID in it.products }
            val purchased = relevant.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            val valid = purchased.firstOrNull { verify(it) }
            val pending = relevant.any { it.purchaseState == Purchase.PurchaseState.PENDING }
            if (valid != null) {
                acceptPurchase(valid)
            } else {
                clearReceipt()
                state = state.copy(active = false, autoRenewing = false, pending = pending, checked = true,
                    message = when {
                        purchased.isNotEmpty() -> tr("Your purchase could not be verified. Please try Restore purchases again or contact Creative Pixels.", "Votre achat n’a pas pu être vérifié. Réessayez Restaurer les achats ou contactez Creative Pixels.")
                        pending -> tr("Google Play is waiting for payment. Access starts after payment is confirmed.", "Google Play attend le paiement. L’accès commence après la confirmation du paiement.")
                        restoring -> tr("No active ParentBell subscription was found. Use the Google Play account that made the purchase.", "Aucun abonnement ParentBell actif n’a été trouvé. Utilisez le compte Google Play qui a effectué l’achat.")
                        else -> ""
                    })
            }
            // A returned but unverifiable purchase must never initiate a second charge.
            if (purchased.isNotEmpty() && valid == null) {
                state = state.copy(purchasing = false)
            } else beforeCheckout?.invoke()
        } }
    }

    private fun verify(purchase: Purchase): Boolean =
        verifier.verify(purchase.originalJson, purchase.signature) &&
            purchase.packageName == BuildConfig.APPLICATION_ID &&
            SubscriptionPolicy.PRODUCT_ID in purchase.products &&
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED && purchase.purchaseToken.isNotBlank()

    private fun acceptPurchase(purchase: Purchase) {
        lastCheckedAt = System.currentTimeMillis()
        state = state.copy(active = true, autoRenewing = purchase.isAutoRenewing, pending = false, checked = true,
            message = tr("Your ParentBell subscription is active.", "Votre abonnement ParentBell est actif."))
        runCatching { receiptFile.writeText(gson.toJson(CachedReceipt(purchase.originalJson, purchase.signature, lastCheckedAt))) }
        acknowledge(purchase)
    }

    private fun acknowledge(purchase: Purchase) {
        if (purchase.isAcknowledged || !acknowledging.add(purchase.purchaseToken)) return
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { result -> onMain {
            acknowledging.remove(purchase.purchaseToken)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                state = state.copy(message = tr("Your purchase is active, but confirmation to Google Play needs another attempt. Tap Restore purchases.", "Votre achat est actif, mais la confirmation à Google Play doit être réessayée. Touchez Restaurer les achats."))
            }
        } }
    }

    private fun restoreOfflineReceipt() {
        runCatching {
            val receipt = gson.fromJson(receiptFile.readText(), CachedReceipt::class.java)
            if (SubscriptionPolicy.cacheIsFresh(receipt.checkedAt, System.currentTimeMillis())) {
                val purchase = Purchase(receipt.json, receipt.signature)
                if (verify(purchase)) {
                    lastCheckedAt = receipt.checkedAt
                    state = state.copy(active = true, autoRenewing = purchase.isAutoRenewing)
                }
            }
        }
    }

    private fun clearReceipt() {
        lastCheckedAt = 0
        runCatching { receiptFile.delete() }
    }

    private fun onMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (!closed) action()
        } else main.post { if (!closed) action() }
    }

    private fun billingError(code: Int): String = when (code) {
        BillingClient.BillingResponseCode.NETWORK_ERROR,
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> tr("Google Play cannot be reached. Check your connection and try again.", "Google Play est inaccessible. Vérifiez votre connexion et réessayez.")
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> tr("Subscriptions are unavailable on this device or Google Play account. Install ParentBell from Google Play and try again.", "Les abonnements ne sont pas offerts sur cet appareil ou compte Google Play. Installez ParentBell depuis Google Play et réessayez.")
        else -> tr("Google Play could not complete this request. Please try again.", "Google Play n’a pas pu traiter cette demande. Réessayez.")
    }

    override fun onCleared() {
        closed = true
        client.endConnection()
        super.onCleared()
    }
}
