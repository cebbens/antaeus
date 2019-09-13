package io.pleo.antaeus.core.external

import io.pleo.antaeus.models.Invoice

/**
 *  This is the payment provider. It is a "mock" of an external service that you can pretend runs on another system.
 *  With this API you can ask customers to pay an invoice.
 *
 *  This mock will succeed if the customer has enough money in their balance, however the documentation lays out
 *  scenarios in which paying an invoice could fail.
 */
interface PaymentProvider {

    /**
     * Charge a customer's account the amount from the invoice.
     *
     * @param invoice [Invoice] to be charged.
     *
     * @return `true` when the customer account was successfully charged the given amount. `false` when the customer
     * account balance did not allow the charge.
     *
     * @throws `CustomerNotFoundException` when no customer has the given id.
     * @throws `CurrencyMismatchException` when the currency does not match the customer account.
     * @throws `NetworkException` when a network error happens.
     */
    fun charge(invoice: Invoice): Boolean
}
