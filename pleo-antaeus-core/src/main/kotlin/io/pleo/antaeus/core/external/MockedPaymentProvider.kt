package io.pleo.antaeus.core.external

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 *  This is the payment provider. It is a "mock" of an external service that you can pretend runs on another system.
 */
class MockedPaymentProvider(
        private val invoiceService: InvoiceService,
        private val customerService: CustomerService)
    : PaymentProvider {

    override fun charge(invoice: Invoice): Boolean {
        logger.info("Charging invoice #${invoice.id}...")

        // Fetch invoice customer, and fail if not found
        val customer: Customer = customerService.fetch(invoice.customerId)

        // Verify that the invoice currency matches the customer account currency, and fail otherwise
        if (invoice.amount.currency != customer.currency) {
            throw CurrencyMismatchException(invoiceId = invoice.id, customerId = invoice.customerId)
        }

        // Simulate a network failure (0: network error)
        if (Random.nextInt(0, 100) == 0) {
            throw NetworkException()
        }

        // Simulate whether or not it successfully charged (0: could not be charged)
        val result =  Random.nextInt(0, 10) != 0

        if (result) {
            // Update invoice status to paid
            invoiceService.updateStatus(invoice.id, InvoiceStatus.PAID)
            logger.info("OK! :)")
        }
        else {
            logger.info("NOT charged! :(")
        }

        return result
    }
}