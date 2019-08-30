package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
import io.javalin.http.InternalServerErrorResponse
import io.javalin.http.NotFoundResponse
import io.pleo.antaeus.core.exceptions.BillingException
import io.pleo.antaeus.core.exceptions.EntityNotFoundException
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.billing.BillingService
import io.pleo.antaeus.core.services.billing.strategy.BillingStrategy
import mu.KotlinLogging
import java.text.ParseException

private val logger = KotlinLogging.logger {}
private const val STRATEGY_PARAM = "strategy"

/**
 * Configures the rest app along with basic exception handling and URL endpoints.
 */
class AntaeusRest(
        private val invoiceService: InvoiceService,
        private val customerService: CustomerService,
        private val billingService: BillingService)
    : Runnable {

    override fun run() {
        app.start(7000)
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create()
        // ParseException: return 400 HTTP status code
        .exception(ParseException::class.java) { e, ctx ->
            logger.error(e.message)
            ctx.status(400).json(BadRequestResponse(e.message.toString()).apply { stackTrace = emptyArray() })
        }
        // IllegalArgumentException: return 400 HTTP status code
        .exception(IllegalArgumentException::class.java) { e, ctx ->
            logger.error(e.message)
            ctx.status(400).json(BadRequestResponse(e.message.toString()).apply { stackTrace = emptyArray() })
        }
        // InvoiceNotFoundException: return 404 HTTP status code
        .exception(EntityNotFoundException::class.java) { e, ctx ->
            logger.error(e.message)
            ctx.status(404).json(NotFoundResponse(e.message.toString()).apply { stackTrace = emptyArray() })
        }
        // Unexpected exception: return HTTP 500 status code
        .exception(BillingException::class.java) { e, ctx ->
            logger.error(e.message)
            ctx.status(500).json(InternalServerErrorResponse(e.message.toString()).apply { stackTrace = emptyArray() })
        }
        // Unexpected exception: return HTTP 500 status code
        .exception(Exception::class.java) { e, ctx ->
            logger.error(e.message)
            ctx.status(500).json(InternalServerErrorResponse(e.message.toString()).apply { stackTrace = emptyArray() })
        }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
           path("rest") {
               // Route to check whether the app is running
               // URL: /rest/health
               get("health") {
                   it.json("ok")
               }

               // V1
               path("v1") {
                   path("invoices") {
                       // URL: /rest/v1/invoices
                       get {
                           it.json(invoiceService.fetchAll())
                       }

                       // URL: /rest/v1/invoices/{:id}
                       get(":id") {
                          it.json(invoiceService.fetch(it.pathParam("id").toInt()))
                       }
                   }

                   path("customers") {
                       // URL: /rest/v1/customers
                       get {
                           it.json(customerService.fetchAll())
                       }

                       // URL: /rest/v1/customers/{:id}
                       get(":id") {
                           it.json(customerService.fetch(it.pathParam("id").toInt()))
                       }
                   }

                   path("billing") {
                       // URL: /rest/v1/billing[?strategy={scheduled|simple}][&cron={cron_exp}]
                       post {
                           // Get 'strategy' param and defaults to 'scheduled' if absent
                           val strategyParam = it.queryParam(STRATEGY_PARAM) ?: BillingStrategy.Type.SCHEDULED.name.toLowerCase()

                           val strategy = BillingStrategy.Type.valueOf(strategyParam.toUpperCase())

                           val result = billingService.bill(strategy, it.queryParamMap())

                           if (result) it.status(201).json(mapOf("status" to "executed"))
                           else it.status(500).json(mapOf("status" to "error"))
                       }
                   }
               }
           }
        }
    }
}
