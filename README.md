## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Design

Given that a billing process could change when and how it executes, it was designed to support both.

The _when_ part was designed to be configurable through a new API endpoint (`/billing`) which takes appropriate parameters to configure it.

The _how_ part was designed to support different implementations which can be easily hooked through a billing strategy, and configured on the new API endpoint.

## Implementation

### API Endpoint

A new API endpoint `/billing` was create which currently support two parameters, `strategy` and `cron` (only for `scheduled` strategy).

###### Usage

`_POST /rest/v1/billing[?strategy={scheduled|simple}][&cron={cron_exp}]_`

**Note**: If `strategy` is neither of the supported ones, an `IllegalArgumentException` exception will be thrown.

### Strategy

Even though the requirement stated to implement a billing process scheduled for the first of the month, I went a step further and added a new strategy just as an example.

Hence, a new interface `BillingStrategy` was created which must be implemented by every billing implementation. Currently, there are two:

##### Scheduled
CRON based scheduled billing strategy by means of [Quartz Scheduler](http://www.quartz-scheduler.org).
It schedules a CRON job and 're-fire immediately' if a `JobExecutionException` it thrown (**Note**: This could lead to many jobs waiting to be consumed once resumed).

The `Scheduler` is created only once and stored within _Quartz_, and in subsequently calls it is retrieved. Moreover, the `Trigger` and `JobDetail` built remains the same, the only thing that could chance is the _CRON expression_. Hence, this is a stateless implementation which can be created on each call.

Additionally, it supports re-scheduling of the same job with a new CRON expression sent over the new API endpoint. Sending the CRON expression is optional and if absent it defaults to `0 0 0 1 * ?` - every first of the month at midnight.

Invoice charging is delegated to `PaymentProvider`.

This is the default implementation if none is specified.

###### Usage

* `POST /rest/v1/billing?strategy=scheduled&cron={cron_exp}`
* `POST /rest/v1/billing?strategy=scheduled`    ->  CRON expression defaults to `0 0 0 1 * ?`
* `POST /rest/v1/billing?cron={cron_exp}`       ->  Strategy defaults to `scheduled`
* `POST /rest/v1/billing`                       ->  Strategy defaults to `scheduled` and CRON expression defaults to `0 0 0 1 * ?`

##### Simple

Simple billing strategy which directly tries to charge invoices. Fetches all pending invoices, and try to charge them by delegating to a mocked external "third-party" `PaymentProvider`.

###### Usage

`POST /rest/v1/billing?strategy=simple`

#### Add New Strategy

It consist of three simple steps:
* Create the strategy by implementing the `BillingStrategy` interface.
* Add a new enumeration element to `BillingStrategy.Type`.
* Map the implementation and enumeration element within `BillingService` using any needed parameters coming from the API endpoint.

### Payment Provider

`MockedPaymentProvider` was created as a mocked implementation of `PaymentProvider` to represent a third-party external service that one can pretend to run on another system.

It only function is to charge a customer's account the amount from the passed invoice.

This mock will succeed if the customer has enough money in their balance, however the documentation lays out scenarios in which paying an invoice could fail.

It will return `true` when the customer account was successfully charged the given amount. `false` otherwise (when the customer account balance did not allow the charge). This is simulated with a random.

It could throw different exception:
* `CustomerNotFoundException` when no customer has the given ID.
* `CurrencyMismatchException` when the currency does not match the customer account currency.
* `NetworkException` when a network error happens (simulated again with another random). 

### Testing

Unit tests were added for `MockedPaymentProvider` and `InvoiceService`.

### Miscellaneous

All libraries where updated to its latest versions, including Javalin and Kotlin Standard. Bug fixes and enhancements are always welcome as long as they do not affect current proper and correct app functioning (which in this case it does not 😉).

### ToDos

There are room for improvement:
##### REST Layer
* Add more fine-grained validation (take new).
* Enhance (even more) exception mapping.
* Enhance API responses.

## Developing

Requirements:
- \>= Java 11 environment

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.


*Running through docker*

Install docker for your platform

```
make docker-run
```

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```


### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the "rest api" models used throughout the application.
|
├── pleo-antaeus-rest
|        Entry point for REST API. This is where the routes are defined.
└──
```

### Main Libraries and Dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io) - Simple web framework (for REST)
* [Kotlin Logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5) - Testing framework
* [MockK](https://mockk.io) - Mocking library
* [SQLite 3](https://sqlite.org/index.html) - Database storage engine
* [Quartz Scheduler](http://www.quartz-scheduler.org) - Scheduling engine

Happy hacking 😁!
