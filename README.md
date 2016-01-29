# Akka HTTP and ZeroMQ 

**TODO**: project description

## Overview

**TODO**: application layout image

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

