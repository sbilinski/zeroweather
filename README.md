# Akka HTTP and ZeroMQ 

The goal of this project is to create a template for efficient [request-reply](http://rfc.zeromq.org/spec:28) communication over **ZeroMQ** in an **Akka** environment. It's a work in progress - contributions and suggestions are more than welcome!

## Overview

![Application layout](/doc/zeromq_application_layout.png)

|Module       |Role|
|-------------|----|
|proxy        |A RESTful web service for handling user requests.|
|supplier     |An internal service, which provides data from some external source.|
|message      |Domain-related messages, which are sent over ZeroMQ.|
|communication|Socket actors for handling communication over ZeroMQ. Base implementation was taken from the [spark-kernel](https://github.com/ibm-et/spark-kernel) project.|

## Basic ZeroMQ concepts

Check the [socket](http://api.zeromq.org/4-1:zmq-socket) man page for full contract description.
 
A `DEALER` socket is defined as follows:

> A socket of type ZMQ_DEALER is an advanced pattern used for extending request/reply sockets. Each message sent is round-robined among all connected peers, and each message received is fair-queued from all connected peers.
  
A `ROUTER` socket is defined as follows:

> A socket of type ZMQ_ROUTER is an advanced socket type used for extending request/reply sockets. When receiving messages a ZMQ_ROUTER socket shall prepend a message part containing the identity of the originating peer to the message before passing it to the application. Messages received are fair-queued from among all connected peers. When sending messages a ZMQ_ROUTER socket shall remove the first part of the message and use it to determine the identity of the peer the message shall be routed to.

We're going to use a `DEALER` socket within the **proxy** app and a `ROUTER` socket for the **supplier** app.

## Development mode

For rapid development feedback use the [sbt-revolver](https://github.com/spray/sbt-revolver) plugin. To start the **supplier** process:

    sbt ~supplier/re-start

To start the **proxy** process:

    sbt ~proxy/re-start

