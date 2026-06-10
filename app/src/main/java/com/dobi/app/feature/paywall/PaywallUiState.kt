package com.dobi.app.feature.paywall

data class PaywallUiState(
    val selectedTab: PaywallTab = PaywallTab.TRIAL,
    val selectedPlan: PaywallPlan = PaywallPlan.TRIAL,
    val isLoading: Boolean = false,
    val isCheckingSubscription: Boolean = true,
    val error: String? = null,
    val checkoutData: CheckoutData? = null,
    val navigateToHome: Boolean = false,
    val referralCode: String = "",
    val referralDiscount: Int = 0,
    val referralApplied: Boolean = false,
    val referralError: String? = null,
    val paymentFailed: Boolean = false,
    val paymentFailureReason: String = "",
)

enum class PaywallTab { AUTOPAY, PAY_ONCE, TRIAL }

enum class PaywallPlan(
    val planType: String,
    val label: String,
    val durationLabel: String,
    val priceRs: Int,
    val originalPriceRs: Int?,
    val savingLabel: String?,
    val isMostPopular: Boolean,
    val ctaOriginalLabel: String?,
    val ctaDiscountedLabel: String,
) {
    MONTHLY(
        planType          = "monthly",
        label             = "Monthly",
        durationLabel     = "per month",
        priceRs           = 149,
        originalPriceRs   = null,
        savingLabel       = null,
        isMostPopular     = false,
        ctaOriginalLabel  = null,
        ctaDiscountedLabel = "₹149/month",
    ),
    QUARTERLY(
        planType          = "quarterly",
        label             = "Quarterly",
        durationLabel     = "for 3 months",
        priceRs           = 249,
        originalPriceRs   = 447,
        savingLabel       = "Save 44%",
        isMostPopular     = true,
        ctaOriginalLabel  = "₹447",
        ctaDiscountedLabel = "₹249",
    ),
    HALFYEARLY(
        planType          = "halfyearly",
        label             = "Half Yearly",
        durationLabel     = "for 6 months",
        priceRs           = 449,
        originalPriceRs   = 894,
        savingLabel       = "Save 50%",
        isMostPopular     = false,
        ctaOriginalLabel  = "₹894",
        ctaDiscountedLabel = "₹449",
    ),
    TRIAL(
        planType          = "trial",
        label             = "Trial",
        durationLabel     = "then ₹149/month",
        priceRs           = 1,
        originalPriceRs   = null,
        savingLabel       = "Cancel anytime",
        isMostPopular     = false,
        ctaOriginalLabel  = null,
        ctaDiscountedLabel = "₹1",
    ),
}

data class CheckoutData(
    val subscriptionId: String,
    val keyId: String,
    val userPhone: String? = null,
    val userEmail: String? = null,
)
