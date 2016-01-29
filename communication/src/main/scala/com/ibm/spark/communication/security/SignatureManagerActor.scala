/*
 * Copyright 2014 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.spark.communication.security

import akka.actor.{ Props, ActorRef, Actor }
import akka.util.Timeout
import com.ibm.spark.communication.utils.OrderedSupport
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import akka.pattern.ask
import akka.pattern.pipe

class SignatureManagerActor(
    key: String, scheme: String
) extends Actor with LazyLogging with OrderedSupport {
  private val hmac = Hmac(key, HmacAlgorithm(scheme))

  def this(key: String) = this(key, HmacAlgorithm.SHA256.toString)

  // NOTE: Required to provide the execution context for futures with akka
  import context._

  // NOTE: Required for ask (?) to function... maybe can define elsewhere?
  implicit val timeout = Timeout(5.seconds)

  //
  // List of child actors that the signature manager contains
  //
  private var signatureChecker: ActorRef = _
  private var signatureProducer: ActorRef = _

  /**
   * Initializes all child actors performing tasks for the interpreter.
   */
  override def preStart() = {
    signatureChecker = context.actorOf(
      Props(classOf[SignatureCheckerActor], hmac),
      name = SignatureManagerChildActorType.SignatureChecker.toString
    )
    signatureProducer = context.actorOf(
      Props(classOf[SignatureProducerActor], hmac),
      name = SignatureManagerChildActorType.SignatureProducer.toString
    )
  }

  override def receive: Receive = {
    // Check blob strings for matching digest
    case (signature: String, blob: Seq[_]) =>
      startProcessing()
      val destActor = sender()
      val sigFuture = signatureChecker ? ((signature, blob))

      sigFuture foreach {
        case isValid =>
          destActor ! isValid
          finishedProcessing()
      }
  }

  /**
   * Defines the types that will be stashed by {@link #waiting() waiting}
   * while the Actor is in processing state.
   * @return
   */
  override def orderedTypes(): Seq[Class[_]] = Seq(
    classOf[(String, Seq[_])]
  )
}

