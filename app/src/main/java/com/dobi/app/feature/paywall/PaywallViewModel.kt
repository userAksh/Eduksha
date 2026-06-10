package com.dobi.app.feature.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.analytics.AnalyticsTracker
import com.dobi.app.analytics.Events
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState = _uiState.asStateFlow()

    private val functions = FirebaseFunctions.getInstance("asia-south1")
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    init {
        analytics.log(Events.PAYWALL_SHOWN)
        viewModelScope.launch { checkExistingSubscription() }
        viewModelScope.launch {
            PaymentEventBus.results.collect { result ->
                when (result) {
                    is PaymentResult.Success -> {
                        analytics.log(Events.SUBSCRIPTION_STARTED) {
                            putString("plan", _uiState.value.selectedPlan.planType)
                        }
                        // Write isPremium immediately — both iOS and Android read
                        // subscriptions/{uid}.isPremium, so this single write makes
                        // the subscription visible cross-platform without waiting for
                        // the Razorpay webhook.
                        auth.currentUser?.uid?.let { uid ->
                            firestore.collection("subscriptions").document(uid)
                                .set(mapOf("isPremium" to true, "status" to "active"), SetOptions.merge())
                        }
                        _uiState.update { it.copy(navigateToHome = true) }
                    }
                    is PaymentResult.Failure -> {
                        // Code 2 = Checkout.PAYMENT_CANCELED — user dismissed the
                        // sheet themselves. Treat as silent cancel, not a failure.
                        if (result.code == 2) return@collect
                        _uiState.update {
                            it.copy(
                                paymentFailed = true,
                                paymentFailureReason = result.description.ifBlank { "Payment failed. Please try again." },
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun checkExistingSubscription() {
        try {
            val userId = auth.currentUser?.uid ?: return
            val doc = firestore.collection("subscriptions").document(userId).get().await()
            if (doc.getBoolean("isPremium") == true) {
                _uiState.update { it.copy(navigateToHome = true) }
            }
        } catch (_: Exception) {
            // No subscription doc or rules deny access — just show the paywall
        } finally {
            _uiState.update { it.copy(isCheckingSubscription = false) }
        }
    }

    fun selectTab(tab: PaywallTab) {
        _uiState.update {
            it.copy(
                selectedTab = tab,
                selectedPlan = if (tab == PaywallTab.TRIAL) PaywallPlan.TRIAL else PaywallPlan.QUARTERLY,
                error = null,
            )
        }
    }

    fun selectPlan(plan: PaywallPlan) {
        _uiState.update { it.copy(selectedPlan = plan, error = null) }
    }

    fun onReferralCodeChanged(code: String) {
        _uiState.update { it.copy(referralCode = code, referralError = null) }
    }

    fun applyReferralCode() {
        val code = _uiState.value.referralCode.trim()
        if (code.isBlank()) {
            _uiState.update { it.copy(referralError = "Please enter a referral code") }
            return
        }
        _uiState.update { it.copy(referralApplied = true, referralError = null) }
    }

    fun removeReferralCode() {
        _uiState.update {
            it.copy(
                referralCode = "",
                referralApplied = false,
                referralDiscount = 0,
                referralError = null,
            )
        }
    }

    fun onCheckoutConsumed() {
        _uiState.update { it.copy(checkoutData = null) }
    }

    fun onNavigationConsumed() {
        _uiState.update { it.copy(navigateToHome = false) }
    }

    fun onCheckoutError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun retryPayment() {
        _uiState.update { it.copy(paymentFailed = false, paymentFailureReason = "") }
        startCheckout()
    }

    fun dismissPaymentFailed() {
        _uiState.update { it.copy(paymentFailed = false, paymentFailureReason = "") }
    }

    fun startCheckout() {
        val userId = auth.currentUser?.uid ?: return
        val plan = _uiState.value.selectedPlan
        analytics.log(Events.CHECKOUT_STARTED) { putString("plan", plan.planType) }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val referralCode = _uiState.value.referralCode.trim().takeIf { it.isNotBlank() }
                val data = mapOf(
                    "planType" to plan.planType,
                    "tabType" to _uiState.value.selectedTab.name.lowercase(),
                    "userId" to userId,
                    "referralCode" to referralCode,
                )
                val result = functions
                    .getHttpsCallable("createSubscription")
                    .call(data)
                    .await()
                    .data as? Map<*, *>

                val subscriptionId = result?.get("subscriptionId") as? String
                val keyId = result?.get("keyId") as? String

                if (subscriptionId == null || keyId == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to create subscription. Please try again.") }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        checkoutData = CheckoutData(
                    subscriptionId = subscriptionId,
                    keyId = keyId,
                    userPhone = auth.currentUser?.phoneNumber,
                    userEmail = "${userId}@eduksha.app",
                ),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Something went wrong") }
            }
        }
    }
}
