package io.pleo.antaeus.core.external

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

class MockedPaymentProvider(private val dal: AntaeusDal) : PaymentProvider {

    override fun charge(invoice: Invoice): Boolean {
        logger.info("Charging invoice #${invoice.id}...")

        // Fetch invoice customer, and fail if not found
        val customer: Customer = dal.fetchCustomer(invoice.customerId) ?: throw CustomerNotFoundException(invoice.customerId)

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
            dal.updateInvoiceStatus(invoice.id, InvoiceStatus.PAID)
            logger.info("OK! :)")
        }
        else {
            logger.info("ERROR! :(")
        }

        return result
    }
}