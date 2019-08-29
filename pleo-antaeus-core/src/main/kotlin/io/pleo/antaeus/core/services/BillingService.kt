package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider

class BillingService(private val paymentProvider: PaymentProvider, private val invoiceService: InvoiceService) {

    fun bill(): Boolean {
        println("Billing PENDING invoices...")

        // Charge all PENDING invoices
        invoiceService.fetchAllPending().forEach { paymentProvider.charge(it) }

        println("Billing completed successfully! :)")
        return true
    }
}