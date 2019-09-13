package io.pleo.antaeus.core.services.billing.strategy

import io.pleo.antaeus.core.exceptions.BillingException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.InvoiceService
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Simple billing strategy which directly tries to charge invoices.
 *
 * Invoice charging is delegated to [PaymentProvider].
 *
 * @param paymentProvider External service responsible of charging invoices.
 * @param invoiceService Invoice service.
 */
class SimpleBillingStrategy(
        private val paymentProvider: PaymentProvider,
        private val invoiceService: InvoiceService)
    : BillingStrategy {

    override fun bill(): Boolean {
        try {
            // Charge any pending invoice
            invoiceService.fetchAllPending().forEach { paymentProvider.charge(it) }
            return true
        }
        catch (e: Exception) {
            logger.error(e) { e.message }
            throw BillingException(e)
        }
    }
}