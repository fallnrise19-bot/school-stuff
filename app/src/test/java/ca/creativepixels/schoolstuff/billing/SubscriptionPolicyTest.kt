package ca.creativepixels.schoolstuff.billing

import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class SubscriptionPolicyTest {
    @Test fun offlineAccessExpiresAndRejectsClockRollback() {
        val checkedAt = 100_000L
        assertTrue(SubscriptionPolicy.cacheIsFresh(checkedAt, checkedAt))
        assertTrue(SubscriptionPolicy.cacheIsFresh(checkedAt, checkedAt + SubscriptionPolicy.OFFLINE_ACCESS_MILLIS - 1))
        assertFalse(SubscriptionPolicy.cacheIsFresh(checkedAt, checkedAt + SubscriptionPolicy.OFFLINE_ACCESS_MILLIS))
        assertFalse(SubscriptionPolicy.cacheIsFresh(checkedAt, checkedAt - 1))
        assertFalse(SubscriptionPolicy.cacheIsFresh(0, checkedAt))
    }

    @Test fun checkoutRejectsTrialsAnnualPlansPrepaidAndInstallments() {
        assertTrue(SubscriptionPolicy.isMonthlyPlan("monthly", null, listOf("P1M"), listOf(1), listOf(3990000), false))
        assertFalse(SubscriptionPolicy.isMonthlyPlan("monthly", "trial", listOf("P1M"), listOf(1), listOf(3990000), false))
        assertFalse(SubscriptionPolicy.isMonthlyPlan("monthly", null, listOf("P1Y"), listOf(1), listOf(3990000), false))
        assertFalse(SubscriptionPolicy.isMonthlyPlan("monthly", null, listOf("P1M"), listOf(3), listOf(3990000), false))
        assertFalse(SubscriptionPolicy.isMonthlyPlan("monthly", null, listOf("P1M"), listOf(1), listOf(3990000), true))
        assertFalse(SubscriptionPolicy.isMonthlyPlan("monthly", null, listOf("P1M"), listOf(1), listOf(0), false))
        assertFalse(SubscriptionPolicy.isMonthlyPlan("monthly", null, listOf("P1W", "P1M"), listOf(2, 1), listOf(0, 3990000), false))
        assertFalse(SubscriptionPolicy.isMonthlyPlan("annual", null, listOf("P1M"), listOf(1), listOf(3990000), false))
    }

    @Test fun signatureVerificationRejectsTamperingWrongKeyAndMissingConfiguration() {
        val generator = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
        val pair = generator.generateKeyPair()
        val other = generator.generateKeyPair()
        val data = "{\"productId\":\"parentbell_monthly\",\"purchaseState\":0}"
        val signature = Signature.getInstance("SHA1withRSA").run {
            initSign(pair.private)
            update(data.toByteArray(Charsets.UTF_8))
            Base64.getEncoder().encodeToString(sign())
        }
        val verifier = PurchaseSignatureVerifier(Base64.getEncoder().encodeToString(pair.public.encoded))
        assertTrue(verifier.isConfigured)
        assertTrue(verifier.verify(data, signature))
        assertFalse(verifier.verify(data.replace("0", "1"), signature))
        assertFalse(verifier.verify(data, "not a signature"))
        assertFalse(PurchaseSignatureVerifier(Base64.getEncoder().encodeToString(other.public.encoded)).verify(data, signature))
        assertFalse(PurchaseSignatureVerifier("").isConfigured)
        assertFalse(PurchaseSignatureVerifier("").verify(data, signature))
    }

    @Test fun purchaseButtonRequiresVerifiedConfigurationAndKnownOwnership() {
        val ready = SubscriptionState(verificationConfigured = true, connected = true, checked = true, planAvailable = true)
        assertTrue(ready.canSubscribe)
        assertFalse(ready.copy(verificationConfigured = false).canSubscribe)
        assertFalse(ready.copy(connected = false).canSubscribe)
        assertFalse(ready.copy(checked = false).canSubscribe)
        assertFalse(ready.copy(active = true).canSubscribe)
        assertFalse(ready.copy(pending = true).canSubscribe)
        assertFalse(ready.copy(checking = true).canSubscribe)
        assertFalse(ready.copy(purchasing = true).canSubscribe)
        assertFalse(ready.copy(planAvailable = false).canSubscribe)
    }
}
