package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

/**
 * Implements services related to invoices.
 */
class InvoiceService(private val dal: AntaeusDal) {

    fun fetchAll(): List<Invoice> {
       return dal.fetchInvoices()
    }

    fun fetchAllPending(): List<Invoice> {
       return dal.fetchPendingInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun updateStatus(id: Int, status: InvoiceStatus) : Invoice {
        return dal.updateInvoiceStatus(id, status) ?: throw InvoiceNotFoundException(id)
    }
}
