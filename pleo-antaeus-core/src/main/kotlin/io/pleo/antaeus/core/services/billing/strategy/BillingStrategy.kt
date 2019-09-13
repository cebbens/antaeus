package io.pleo.antaeus.core.services.billing.strategy

/**
 * Billing strategy. Currently, it supports CRON Scheduled and Simple strategies.
 */
interface BillingStrategy {

    fun bill(): Boolean

    // Supported billing strategies
    enum class Type {
        SCHEDULED,
        SIMPLE
    }
}
