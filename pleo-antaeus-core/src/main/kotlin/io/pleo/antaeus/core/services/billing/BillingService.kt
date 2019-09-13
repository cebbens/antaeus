package io.pleo.antaeus.core.services.billing

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.billing.strategy.BillingStrategy
import io.pleo.antaeus.core.services.billing.strategy.BillingStrategy.Type.SCHEDULED
import io.pleo.antaeus.core.services.billing.strategy.BillingStrategy.Type.SIMPLE
import io.pleo.antaeus.core.services.billing.strategy.ScheduledBillingStrategy
import io.pleo.antaeus.core.services.billing.strategy.SimpleBillingStrategy
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private const val CRON_PARAM = "cron"

/**
 * Responsible of billing by delegating to [BillingStrategy].
 *
 * @param paymentProvider External service responsible of charging invoices.
 * @param invoiceService Invoice service.
 */
class BillingService(private val paymentProvider: PaymentProvider, private val invoiceService: InvoiceService) {

    fun bill(strategy: BillingStrategy.Type, params: Map<String, List<String>>): Boolean {
        logger.info("${strategy.name} billing started...")
        return when (strategy) {
            SCHEDULED -> ScheduledBillingStrategy(paymentProvider, invoiceService, cron = params[CRON_PARAM]?.first())
            SIMPLE -> SimpleBillingStrategy(paymentProvider, invoiceService)
        }.bill()
    }
}
