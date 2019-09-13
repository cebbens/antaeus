package io.pleo.antaeus.core.services.billing.strategy

import io.pleo.antaeus.core.exceptions.BillingException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.InvoiceService
import mu.KotlinLogging
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import java.time.LocalTime

private val logger = KotlinLogging.logger {}

// Default CRON expression: every first of the month at midnight
private const val DEFAULT_CRON_EXP = "0 0 0 1 * ?"
private const val TRIGGER_NAME = "billingTrigger"
private const val JOB_NAME = "billingJob"

/**
 * CRON scheduled billing strategy by means of Quartz Scheduler. It schedules a CRON job and 're-fire immediately'
 * if a [JobExecutionException] it thrown (Note: This could lead to many jobs waiting to be consumed once resumed).
 *
 * The [Scheduler] is created only once and stored within Quartz, and in subsequently calls it is retrieved. Moreover,
 * the [Trigger] and [JobDetail] built remains the same, the only thing that could chance is the CRON expression.
 * Hence, this is a stateless implementation which can be created on each call.
 *
 * It supports re-scheduling of the same job with a new CRON expression.
 *
 * Invoice charging is delegated to [PaymentProvider].
 *
 * @param paymentProvider External service responsible of charging invoices.
 * @param invoiceService Invoice service.
 * @param cron Optional CRON expression (defaults to "0 0 0 1 * ?": every first of the month at midnight).
 */
class ScheduledBillingStrategy(
        private val paymentProvider: PaymentProvider,
        private val invoiceService: InvoiceService,
        private val cron: String? = DEFAULT_CRON_EXP)
    : BillingStrategy {

    // Create/retrieve scheduler
    private val scheduler: Scheduler = StdSchedulerFactory().scheduler
    private val job: JobDetail
    private val trigger: Trigger

    init {
        // Create Job
        job = JobBuilder.newJob(BillingJob::class.java).withIdentity(JOB_NAME).build()

        // Create Trigger
        trigger = TriggerBuilder.newTrigger()
                .withIdentity(TRIGGER_NAME)
                // CRON Schedule based on received cron expression
                .withSchedule(CronScheduleBuilder.cronScheduleNonvalidatedExpression(cron ?: DEFAULT_CRON_EXP))
                // Pass dependencies to the BillingJob instance
                .usingJobData(JobDataMap(mapOf("invoiceService" to invoiceService, "paymentProvider" to paymentProvider)))
                // Associate Job to Trigger
                .forJob(job)
                .build()
    }

    override fun bill(): Boolean {
        try {
            when {
                // Schedule job
                !scheduler.isStarted -> {
                    scheduler.scheduleJob(job, trigger)
                    scheduler.start()
                }
                // Re-schedule job if scheduler already started
                else -> scheduler.rescheduleJob(TriggerKey.triggerKey(TRIGGER_NAME), trigger)
            }
            return true
        }
        catch (e: Exception) {
            logger.error(e) { "Scheduled billing ERROR!" }
            throw BillingException(e)
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
