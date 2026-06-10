package com.dobi.app.feature.paywall

import kotlinx.coroutines.flow.MutableSharedFlow

object PaymentEventBus {
    val results = MutableSharedFlow<PaymentResult>(extraBufferCapacity = 1)
}

sealed class PaymentResult {
    data class Success(val paymentId: String) : PaymentResult()
    data class Failure(val code: Int, val description: String) : PaymentResult()
}
