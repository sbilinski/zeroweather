# Spark Kernel Communication

This library is a slightly adjusted version of the [spark-kernel/communication](https://github.com/ibm-et/spark-kernel/tree/master/communication) 
module.

* All `com.ibm.spark.utils.LogLike` references were replaced by `com.typesafe.scalalogging.LazyLogging`
* All `KernelMessage` references were removed.
* All `security` components were removed.
* Fixed inbound message processing for the `PUB` socket.
