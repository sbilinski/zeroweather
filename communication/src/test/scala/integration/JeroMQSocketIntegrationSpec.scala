/*
 * Copyright 2015 IBM Corp.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package integration

import java.net.ServerSocket
import java.util.concurrent.ConcurrentLinkedDeque

import com.ibm.spark.communication.SocketManager
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{ Milliseconds, Span }
import org.scalatest.{ Matchers, OneInstancePerTest, FunSpec }
import org.zeromq.ZMQ.Context
import org.zeromq.{ ZMsg, ZMQ }

class JeroMQSocketIntegrationSpec extends FunSpec
    with OneInstancePerTest with Matchers with Eventually {
  implicit override val patienceConfig = PatienceConfig(
    timeout = scaled(Span(800, Milliseconds)),
    interval = scaled(Span(10, Milliseconds))
  )

  private val context = ZMQ.context(0)
  private val socketManager: SocketManager = new SocketManager {
    override protected def newZmqContext(): Context = context
  }
  def getSocketPort = {
    val socket: ServerSocket = new ServerSocket(0)
    socket.close()
    socket.getLocalPort
  }
  describe("JeroMQSocket->ZeroMQSocketRunnable") {
    describe("Request/Reply sockets") {
      it("should be able to communicate") {
        val address = s"inproc://${this.hashCode()}"

        val replyMessages = new ConcurrentLinkedDeque[Seq[String]]()
        val replyCallback: (Seq[String]) => Unit = { msg: Seq[String] =>
          replyMessages.offer(msg)
        }
        val reply = socketManager.newRouterSocket(
          address,
          replyCallback
        )

        eventually {
          reply.isReady should be(true)
        }

        val requestMessages = new ConcurrentLinkedDeque[Seq[String]]()
        val requestCallback: (Seq[String]) => Unit = { msg: Seq[String] =>
          requestMessages.offer(msg)
        }
        val request = socketManager.newDealerSocket(
          address,
          requestCallback
        )

        eventually {
          request.isReady should be(true)
        }

        request.send("Message from the request to the reply")

        eventually {
          replyMessages.size() should be(1)
        }

        socketManager.closeSocket(reply)
        socketManager.closeSocket(request)
      }
    }

    describe("Router/Dealer sockets") {
      it("should be able to communicate") {
        val address = s"inproc://${this.hashCode()}"

        val routerMessages = new ConcurrentLinkedDeque[Seq[String]]()
        val routerCallback: (Seq[String]) => Unit = { msg: Seq[String] =>
          routerMessages.offer(msg)
        }
        val router = socketManager.newRouterSocket(
          address,
          routerCallback
        )

        eventually {
          router.isReady should be(true)
        }

        val dealer = socketManager.newDealerSocket(
          address,
          (_: Seq[String]) => {}
        )

        eventually {
          dealer.isReady should be(true)
        }

        dealer.send("Message from the dealer to the router")

        eventually {
          routerMessages.size() should be(1)
        }

        socketManager.closeSocket(router)
        socketManager.closeSocket(dealer)
      }
    }

    describe("Pub/Sub sockets") {
      it("should be able to communicate") {
        val address = s"inproc://${this.hashCode()}"
        val publisher = socketManager.newPubSocket(
          address
        )

        eventually {
          publisher.isReady should be(true)
        }

        val subscriberMessages = new ConcurrentLinkedDeque[Seq[String]]()
        val subscriberCallback: (Seq[String]) => Unit = { msg: Seq[String] =>
          subscriberMessages.offer(msg)
        }
        val subscriber = socketManager.newSubSocket(
          address,
          subscriberCallback
        )

        eventually {
          subscriber.isReady should be(true)
        }

        publisher.send("Message form the publisher to the subscriber")

        eventually {
          subscriberMessages.size() should be(1)
        }

        socketManager.closeSocket(subscriber)
        socketManager.closeSocket(publisher)
      }
    }
  }
}
