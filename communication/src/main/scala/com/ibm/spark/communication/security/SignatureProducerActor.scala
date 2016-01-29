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

import akka.actor.Actor
import com.ibm.spark.communication.utils.OrderedSupport
import com.typesafe.scalalogging.LazyLogging

/**
 * Constructs a signature from any kernel message received.
 * @param hmac The HMAC to use for signature construction
 */
class SignatureProducerActor(
    private val hmac: Hmac
) extends Actor with LazyLogging with OrderedSupport {
  override def receive: Receive = {
    case _ => ???
  }

  /**
   * Defines the types that will be stashed by {@link #waiting() waiting}
   * while the Actor is in processing state.
   * @return
   */
  override def orderedTypes(): Seq[Class[_]] = ???
}
