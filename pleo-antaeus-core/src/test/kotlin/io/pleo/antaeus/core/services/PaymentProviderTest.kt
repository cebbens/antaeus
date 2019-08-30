package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.external.MockedPaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency.*
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus.PAID
import io.pleo.antaeus.models.InvoiceStatus.PENDING
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class PaymentProviderTest {

    private val dal = mockk<AntaeusDal> {
        every { fetchCustomer(404) } returns null

        every { fetchCustomer(400) } returns Customer(id = 400, currency = SEK)

        every { fetchCustomer(200) } returns Customer(id = 200, currency = DKK)

        every { fetchInvoice(200) } returns
                Invoice(id = 200, customerId = 7, amount = Money(value = BigDecimal.TEN, currency = DKK), status = PENDING)

        every { updateInvoiceStatus(1, PAID) } returns
                Invoice(id = 1, customerId = 200, amount = Money(BigDecimal.TEN, DKK), status = PAID)
    }

    private val paymentProvider = MockedPaymentProvider(
            invoiceService = InvoiceService(dal = dal),
            customerService = CustomerService(dal = dal))

    @Test
    fun `will throw CustomerNotFoundException if customer is not found`() {
        assertThrows<CustomerNotFoundException> {
            val invoice = Invoice(id = 1, customerId = 404, amount = Money(value = BigDecimal.TEN, currency = EUR), status = PENDING)

            paymentProvider.charge(invoice)
        }
    }

    @Test
    fun `will throw CurrencyMismatchException if invoice currency does not match the customer account`() {
        assertThrows<CurrencyMismatchException> {
            val invoice = Invoice(id = 1, customerId = 400, amount = Money(value = BigDecimal.TEN, currency = EUR), status = PENDING)

            paymentProvider.charge(invoice)
        }
    }
}