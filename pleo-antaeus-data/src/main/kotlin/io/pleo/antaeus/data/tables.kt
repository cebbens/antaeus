/*
    Defines database tables and their schemas.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.InvoiceStatus
import org.jetbrains.exposed.sql.Table

object InvoiceTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = enumerationByName("currency", 3, Currency::class)
    val value = decimal("value", 1000, 2)
    val customerId = reference("customer_id", CustomerTable.id)
    val status = enumerationByName("status", 20, InvoiceStatus::class)
}

object CustomerTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = enumerationByName("currency", 3, Currency::class)
}
