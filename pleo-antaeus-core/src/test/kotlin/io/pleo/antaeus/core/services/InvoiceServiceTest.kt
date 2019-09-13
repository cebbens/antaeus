package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency.*
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus.*
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class InvoiceServiceTest {
    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null

        every { fetchPendingInvoices() } returns listOf(
                Invoice(id = 1, customerId = 1, amount = Money(BigDecimal.TEN, EUR), status = PENDING))
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if customer is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `will fetch pending invoices`() {
        val subject = invoiceService.fetchAllPending().first()
        val expectedSubject = Invoice(id = 1, customerId = 1, amount = Money(BigDecimal.TEN, EUR), status = PENDING)

        assertEquals(expectedSubject, subject)
    }
}