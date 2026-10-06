package ca.creativepixels.schoolstuff.billing

import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object SubscriptionPolicy {
    const val PRODUCT_ID = "parentbell_monthly"
    const val BASE_PLAN_ID = "monthly"
    const val OFFLINE_ACCESS_MILLIS = 24L * 60 * 60 * 1000

    fun cacheIsFresh(checkedAt: Long, now: Long): Boolean =
        checkedAt > 0 && now >= checkedAt && now - checkedAt < OFFLINE_ACCESS_MILLIS

    // Accept only the regular monthly auto-renewing plan. No undisclosed trials,
    // introductory phases, prepaid plans, or installment commitments.
    fun isMonthlyPlan(basePlanId: String, offerId: String?, periods: List<String>,
                      recurrenceModes: List<Int>, amounts: List<Long>, hasInstallments: Boolean): Boolean =
        basePlanId == BASE_PLAN_ID && offerId == null && periods == listOf("P1M") &&
            recurrenceModes == listOf(1) && amounts.size == 1 && amounts.single() > 0 && !hasInstallments
}

class PurchaseSignatureVerifier(publicKey: String) {
    private val key = runCatching {
        KeyFactory.getInstance("RSA").generatePublic(
            X509EncodedKeySpec(Base64.getDecoder().decode(publicKey))
        )
    }.getOrNull()

    val isConfigured: Boolean get() = key != null

    fun verify(data: String, signature: String): Boolean = runCatching {
        if (key == null || data.isBlank() || signature.isBlank()) return false
        Signature.getInstance("SHA1withRSA").run {
            initVerify(key)
            update(data.toByteArray(StandardCharsets.UTF_8))
            verify(Base64.getDecoder().decode(signature))
        }
    }.getOrDefault(false)
}
