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
 * @Date: 2024/4/20 22:29
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.rpc.netty

import java.util.concurrent.{ConcurrentHashMap, LinkedBlockingQueue, ThreadPoolExecutor, TimeUnit}

import scala.util.control.NonFatal

import com.github.feilewu.monitor.core.ThreadUtils
import com.github.feilewu.monitor.core.log.Logging
import com.github.feilewu.monitor.core.rpc.RpcEndpoint

private[netty] abstract class MessageLoop(dispatcher: Dispatcher) extends Logging {

  private val active = new LinkedBlockingQueue[Inbox]()

  var stopped = false

  protected val threadPool: ThreadPoolExecutor = null

  def post(endpointName: String, message: InboxMessage): Unit

  def unregister(name: String): Unit

  // Message loop task; should be run in all threads of the message loop's pool.
  protected val receiveLoopRunnable: Runnable = new Runnable() {
    override def run(): Unit = receiveLoop()
  }

  protected final def setActive(inbox: Inbox): Unit = active.offer(inbox)

  private def receiveLoop(): Unit = {
    try {
      while (true) {
        try {
          val inbox = active.take()
          if (inbox == MessageLoop.PoisonPill) {
            // Put PoisonPill back so that other threads can see it.
            setActive(MessageLoop.PoisonPill)
            return
          }
          inbox.process(dispatcher)
        } catch {
          case NonFatal(e) => logError(e.getMessage, e)
        }
      }
    } catch {
      case _: InterruptedException => // exit
      case t: Throwable =>
        try {
          // Re-submit a receive task so that message delivery will still work if
          // UncaughtExceptionHandler decides to not kill JVM.
          threadPool.execute(receiveLoopRunnable)
        } finally {
          throw t
        }
    }
  }

  def stop(): Unit


}

private object MessageLoop {
  /** A poison inbox that indicates the message loop should stop processing messages. */
  val PoisonPill = new Inbox(null, null)
}

private class SharedMessageLoop(dispatcher: Dispatcher,
                                 numUsableCores: Int)
  extends MessageLoop(dispatcher) {

  private val endpoints = new ConcurrentHashMap[String, Inbox]()

  private def getNumOfThreads(): Int = {
    val availableCores =
      if (numUsableCores > 0) numUsableCores else Runtime.getRuntime.availableProcessors()
    math.max(2, availableCores)
  }

  /** Thread pool used for dispatching messages. */
  override protected val threadPool: ThreadPoolExecutor = {
    val numThreads = getNumOfThreads()
    val pool = ThreadUtils.newDaemonFixedThreadPool(numThreads, "dispatcher-event-loop")
    for (i <- 0 until numThreads) {
      pool.execute(receiveLoopRunnable)
    }
    pool
  }

  override def post(endpointName: String, message: InboxMessage): Unit = {
    val inbox = endpoints.get(endpointName)
    inbox.post(message)
    setActive(inbox)
  }

  override def unregister(name: String): Unit = {
    val inbox = endpoints.remove(name)
    if (inbox != null) {
      inbox.stop()
      // Mark active to handle the OnStop message.
      setActive(inbox)
    }
  }

  def register(name: String, endpoint: RpcEndpoint): Unit = {
    val inbox = new Inbox(name, endpoint)
    endpoints.put(name, inbox)
    // Mark active to handle the OnStart message.
    setActive(inbox)
  }

  override def stop(): Unit = {
    synchronized {
      if (!stopped) {
        setActive(MessageLoop.PoisonPill)
        threadPool.shutdown()
        stopped = true
      }
    }
    threadPool.awaitTermination(Long.MaxValue, TimeUnit.MILLISECONDS)
  }
}

class IsolationMessageLoop(dispatcher: Dispatcher)
  extends MessageLoop(dispatcher) {


  override def post(endpointName: String, message: InboxMessage): Unit = {

  }

  override def unregister(name: String): Unit = {

  }

  override def stop(): Unit = {

  }
}
