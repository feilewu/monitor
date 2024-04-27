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
 * @Date: 2024/4/21 13:15
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.rpc.netty

import java.nio.ByteBuffer
import java.util.concurrent.Callable

import scala.util.control.NonFatal

import com.github.feilewu.monitor.core.exception.MonitorException
import com.github.feilewu.monitor.core.rpc.RpcAddress
import com.github.feilewu.monitor.network.client.{RpcResponseCallback, TransportClient}

private[netty] sealed trait OutboxMessage {

  def sendWith(client: TransportClient): Unit

  def onFailure(e: Throwable): Unit

}

private[netty] case class RpcOutboxMessage(
                                            content: ByteBuffer,
                                            _onFailure: (Throwable) => Unit,
                                            _onSuccess: (TransportClient, ByteBuffer) => Unit)
  extends OutboxMessage with RpcResponseCallback {

  private var client: TransportClient = _
  private var requestId: Long = _

  override def sendWith(client: TransportClient): Unit = {
    this.client = client
    this.requestId = client.sendRpc(content, this)
  }

  private[netty] def removeRpcRequest(): Unit = {
    if (client != null) {
      // client.removeRpcRequest(requestId)
    } else {

    }
  }

  def onTimeout(): Unit = {
    removeRpcRequest()
  }

  def onAbort(): Unit = {
    removeRpcRequest()
  }

  override def onFailure(e: Throwable): Unit = {
    _onFailure(e)
  }

  override def onSuccess(response: ByteBuffer): Unit = {
    _onSuccess(client, response)
  }

}



private [monitor] class Outbox(nettyEnv: NettyRpcEnv, val rpcAddress: RpcAddress) {

  outbox => // Give this an alias so we can use it more clearly in closures.

  private val messages = new java.util.LinkedList[OutboxMessage]

  private var client: TransportClient = null

  private var connectFuture: java.util.concurrent.Future[Unit] = null

  private var stopped = false

  private var draining = false

  def send(message: OutboxMessage): Unit = {
    messages.add(message)
    drainOutbox()
  }

  private def drainOutbox(): Unit = {
    var message: OutboxMessage = null
    synchronized {
      if (stopped) {
        return
      }
      if (connectFuture != null) {
        // We are connecting to the remote address, so just exit
        return
      }
      if (client == null) {
        // There is no connect task but client is null, so we need to launch the connect task.
        launchConnectTask()
        return
      }
      if (draining) {
        // There is some thread draining, so just exit
        return
      }
      message = messages.poll()
      if (message == null) {
        return
      }
      draining = true
    }
    while (true) {
      try {
        val _client = synchronized { client }
        if (_client != null) {
          message.sendWith(_client)
        } else {
          assert(stopped)
        }
      } catch {
        case NonFatal(e) =>
          return
      }
      synchronized {
        if (stopped) {
          return
        }
        message = messages.poll()
        if (message == null) {
          draining = false
          return
        }
      }
    }
  }

  private def launchConnectTask(): Unit = {
    connectFuture = nettyEnv.clientConnectionExecutor.submit(new Callable[Unit] {

      override def call(): Unit = {
        try {
          val _client = nettyEnv.createClient(rpcAddress)
          outbox.synchronized {
            client = _client
            if (stopped) {
              // closeClient()
            }
          }
        } catch {
          case ie: InterruptedException =>
            // exit
            return
          case NonFatal(e) =>
            outbox.synchronized { connectFuture = null }
            // handleNetworkFailure(e)
            return
        }
        outbox.synchronized { connectFuture = null }
        // It's possible that no thread is draining now. If we don't drain here, we cannot send the
        // messages until the next message arrives.
        drainOutbox()
      }
    })
  }

  private def closeClient(): Unit = synchronized {
    // Just set client to null. Don't close it in order to reuse the connection.
    client = null
  }

  def stop(): Unit = {
    synchronized {
      if (stopped) {
        return
      }
      stopped = true
      if (connectFuture != null) {
        connectFuture.cancel(true)
      }
      closeClient()
    }
    // We always check `stopped` before updating messages, so here we can make sure no thread will
    // update messages and it's safe to just drain the queue.
    var message = messages.poll()
    while (message != null) {
      message.onFailure(new MonitorException("Message is dropped because Outbox is stopped"))
      message = messages.poll()
    }

  }

}


