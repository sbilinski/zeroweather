# ZeroWeather

The goal of this project is to provide a template for a [brokerless](http://zguide.zeromq.org/php:all#Brokerless-Reliability-Freelance-Pattern) [ZeroMQ](http://zeromq.org/) communication system, which is based on [Akka](http://akka.io/) and [Scala](http://www.scala-lang.org/) at its core.

The **akka-zeromq** extension was [removed](https://github.com/akka/akka/issues/16636) from **Akka** in a recent release. Check the project [roadmap](https://www.typesafe.com/blog/akka-roadmap-update-2014) for an explanation of this decision and a summary of the known issues. For this reason, this project was built on [JeroMQ](https://github.com/zeromq/jeromq) (a pure Java implementation of ZeroMQ) and a [spark-kernel](https://github.com/ibm-et/spark-kernel) module, which was [rolled out](https://github.com/akka/akka/issues/16636) as an **akka-zeromq** replacement by the **spark-kernel** team. This may be subject to change, since the communication module isn't available as a standalone library at the moment and some [minor changes](communication/README.md) were applied on it as well.

Brokerless communication is currently assumed to be running in a similar fashion as ["model 1"](http://zguide.zeromq.org/php:all#Model-One-Simple-Retry-and-Failover) (try and failover). This may also change in the upcoming future.

## Overview

![Application layout](/doc/zeromq_application_layout.png)

|Module       |Role|
|-------------|----|
|proxy        |A RESTful web service for handling user requests.|
|supplier     |An internal service, which provides data from some external source.|
|message      |Domain-related messages, which are sent over ZeroMQ.|
|communication|Socket actors for handling communication over ZeroMQ. Base implementation was taken from the **spark-kernel** project.|

## Deployment

### Development mode

For rapid development feedback use the [sbt-revolver](https://github.com/spray/sbt-revolver) plugin. To start the **supplier** process:

    sbt ~supplier/re-start

To start the **proxy** process:

    sbt ~proxy/re-start

### Testing a distributed setup

To start a **proxy** process with explicit binding and an endpoint list:

    sbt -Dhttp.port=8080 -Dzeromq.endpoints.0=tcp://localhost:5555 -Dzeromq.endpoints.1=tcp://localhost:5556 proxy/run
    
To start a **supplier** process with explicit binding:

    sbt -Dzeromq.endpoint=tcp://localhost:5555 supplier/run
    
