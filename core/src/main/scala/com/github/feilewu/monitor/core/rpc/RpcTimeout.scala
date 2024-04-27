/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * @Author: pf_xu
 * @Date: 2024/4/21 10:33
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.rpc

import scala.concurrent.{Future, TimeoutException}
import scala.concurrent.duration.FiniteDuration

import com.github.feilewu.monitor.core.ThreadUtils


/**
 * An exception thrown if RpcTimeout modifies a `TimeoutException`.
 */
private[rpc] class RpcTimeoutException(message: String, cause: TimeoutException)
  extends TimeoutException(message) { initCause(cause) }

private[monitor] class RpcTimeout(val duration: FiniteDuration, val timeoutProp: String)
  extends Serializable {

  /** Amends the standard message of TimeoutException to include the description */
  private def createRpcTimeoutException(te: TimeoutException): RpcTimeoutException = {
    new RpcTimeoutException(te.getMessage + ". This timeout is controlled by " + timeoutProp, te)
  }

  /**
   * PartialFunction to match a TimeoutException and add the timeout description to the message
   *
   * @note This can be used in the recover callback of a Future to add to a TimeoutException
   * Example:
   *    val timeout = new RpcTimeout(5.milliseconds, "short timeout")
   *    Future(throw new TimeoutException).recover(timeout.addMessageIfTimeout)
   */
  def addMessageIfTimeout[T]: PartialFunction[Throwable, T] = {
    // The exception has already been converted to a RpcTimeoutException so just raise it
    case rte: RpcTimeoutException => throw rte
    // Any other TimeoutException get converted to a RpcTimeoutException with modified message
    case te: TimeoutException => throw createRpcTimeoutException(te)
  }

  /**
   * Wait for the completed result and return it. If the result is not available within this
   * timeout, throw a [[RpcTimeoutException]] to indicate which configuration controls the timeout.
   *
   * @param  future  the `Future` to be awaited
   * @throws RpcTimeoutException if after waiting for the specified time `future`
   *         is still not ready
   */
  def awaitResult[T](future: Future[T]): T = {
    try {
      ThreadUtils.awaitResult(future, duration)
    } catch addMessageIfTimeout
  }
}
