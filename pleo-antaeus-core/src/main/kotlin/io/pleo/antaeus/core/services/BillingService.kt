package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import mu.KotlinLogging
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import java.time.LocalTime

private val logger = KotlinLogging.logger {}

/**
 * Responsible of billing by delegating to [PaymentProvider].
 *
 * @param paymentProvider External service responsible of charging invoices.
 * @param invoiceService Invoice service.
 * @param cron Optional CRON expression (defaults to "0 0 0 1 * ?": every first of the month at midnight).
 */
class BillingService(paymentProvider: PaymentProvider, invoiceService: InvoiceService, cron: String? = "0 0 0 1 * ?") {

    // Create/retrieve scheduler
    private val scheduler: Scheduler = StdSchedulerFactory().scheduler
    private val job: JobDetail
    private val trigger: Trigger
    private val triggerName: String = "billingTrigger"

    init {
        // Create Job
        job = JobBuilder.newJob(BillingJob::class.java).withIdentity("billingJob").build()

        // Create Trigger
        trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerName)
                // CRON Schedule based on received cron expression
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                // Pass dependencies to the BillingJob instance
                .usingJobData(JobDataMap(mapOf("invoiceService" to invoiceService, "paymentProvider" to paymentProvider)))
                // Associate Job to Trigger
                .forJob(job)
                .build()
    }

    fun bill(): Boolean {
        try {
            when {
                // Schedule job
                !scheduler.isStarted -> {
                    scheduler.scheduleJob(job, trigger)
                    scheduler.start()
                }
                // Re-schedule job if scheduler already started
                else -> scheduler.rescheduleJob(TriggerKey.triggerKey(triggerName), trigger)
            }
            return true
        }
        catch (e: Exception) {
            logger.error(e) { "Scheduling ERROR!" }
            return false
        }
    }

    // Used by JobBuilder to create new instances
    class BillingJob : Job {
        override fun execute(context: JobExecutionContext) {
            logger.info("Billing job executed @${LocalTime.now()}")

            // Retrieve dependencies from context
            val invoiceService = context.mergedJobDataMap["invoiceService"] as InvoiceService
            val paymentProvider = context.mergedJobDataMap["paymentProvider"] as PaymentProvider

            try {
                // Charge any pending invoice
                invoiceService.fetchAllPending().forEach { paymentProvider.charge(it) }
            }
            catch (e: Exception) {
                // Create a JobExecutionException with the given underlying exception, and 're-fire immediately'
                throw JobExecutionException(e, true)
            }
        }
    }
}
