package com.dobi.app.feature.paywall

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dobi.app.analytics.AnalyticsTracker
import com.dobi.app.analytics.Events
import com.dobi.app.data.local.UserPreferences
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
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private companion object {
        const val TAG = "PaywallVM"
    }

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState = _uiState.asStateFlow()

    private val functions = FirebaseFunctions.getInstance("asia-south1")
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    init {
        Log.d(TAG, "init: paywall shown, plan=${_uiState.value.selectedPlan.planType}")
        analytics.log(Events.PAYWALL_SHOWN)
        viewModelScope.launch { checkExistingSubscription() }
        viewModelScope.launch {
            PaymentEventBus.results.collect { result ->
                when (result) {
                    is PaymentResult.Success -> {
                        Log.d(TAG, "payment success: paymentId=${result.paymentId}")
                        analytics.log(Events.SUBSCRIPTION_STARTED) {
                            putString("plan", _uiState.value.selectedPlan.planType)
                        }
                        // Write isPremium immediately — both iOS and Android read
                        // subscriptions/{uid}.isPremium, so this single write makes
                        // the subscription visible cross-platform without waiting for
                        // the Razorpay webhook.
                        auth.currentUser?.uid?.let { uid ->
                            Log.d(TAG, "writing isPremium=true to subscriptions/$uid")
                            firestore.collection("subscriptions").document(uid)
                                .set(mapOf("isPremium" to true, "status" to "active"), SetOptions.merge())
                        }
                        // Persist the welcome banner across process restarts.
                        viewModelScope.launch { userPreferences.markJustSubscribed() }
                        _uiState.update { it.copy(navigateToHome = true) }
                    }
                    is PaymentResult.Failure -> {
                        // Code 2 = Checkout.PAYMENT_CANCELED — user dismissed the
                        // sheet themselves. Treat as silent cancel, not a failure.
                        if (result.code == 2) {
                            Log.d(TAG, "payment canceled by user")
                            analytics.log(Events.PAYMENT_CANCELED) {
                                putString("plan", _uiState.value.selectedPlan.planType)
                            }
                            return@collect
                        }
                        Log.w(TAG, "payment failed: code=${result.code} reason=${result.description}")
                        analytics.log(Events.PAYMENT_FAILED) {
                            putString("plan", _uiState.value.selectedPlan.planType)
                            putInt("error_code", result.code)
                            putString("reason", result.description)
                        }
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
            val userId = auth.currentUser?.uid ?: run {
                Log.w(TAG, "checkExistingSubscription: no authenticated user")
                return
            }
            Log.d(TAG, "checkExistingSubscription: checking subscriptions/$userId")
            val doc = firestore.collection("subscriptions").document(userId).get().await()
            val isPremium = doc.getBoolean("isPremium") == true
            Log.d(TAG, "checkExistingSubscription: isPremium=$isPremium")
            if (isPremium) {
                _uiState.update { it.copy(navigateToHome = true) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "checkExistingSubscription: failed (will show paywall)", e)
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
        Log.e(TAG, "onCheckoutError: $message")
        analytics.log(Events.CHECKOUT_ERROR) {
            putString("plan", _uiState.value.selectedPlan.planType)
            putString("error", message)
        }
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
        val userId = auth.currentUser?.uid ?: run {
            Log.w(TAG, "startCheckout: no authenticated user")
            return
        }
        val plan = _uiState.value.selectedPlan
        Log.d(TAG, "startCheckout: plan=${plan.planType} tab=${_uiState.value.selectedTab.name}")
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
                Log.d(TAG, "startCheckout: calling createSubscription referralCode=$referralCode")
                val result = functions
                    .getHttpsCallable("createSubscription")
                    .call(data)
                    .await()
                    .data as? Map<*, *>

                val subscriptionId = result?.get("subscriptionId") as? String
                val orderId = result?.get("orderId") as? String
                val keyId = result?.get("keyId") as? String
                Log.d(TAG, "startCheckout: response subscriptionId=$subscriptionId orderId=$orderId keyId=${keyId?.take(8)}…")

                if ((subscriptionId == null && orderId == null) || keyId == null) {
                    Log.w(TAG, "startCheckout: null IDs from function — raw result=$result")
                    _uiState.update { it.copy(isLoading = false, error = "Failed to create subscription. Please try again.") }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        checkoutData = CheckoutData(
                            subscriptionId = subscriptionId,
                            orderId = orderId,
                            keyId = keyId,
                            userPhone = auth.currentUser?.phoneNumber
                                ?.removePrefix("+91"),
                            userEmail = "${userId}@eduksha.app",
                        ),
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "startCheckout: createSubscription threw", e)
                analytics.log(Events.CHECKOUT_ERROR) {
                    putString("plan", plan.planType)
                    putString("error", e.message ?: "unknown")
                }
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Something went wrong") }
            }
        }
    }
}
