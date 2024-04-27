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
 * @Date: 2024/4/20 22:18
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.rpc.netty

import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import scala.concurrent.Promise
import scala.jdk.CollectionConverters.ConcurrentMapHasAsScala
import scala.util.control.NonFatal

import com.github.feilewu.monitor.core.exception.MonitorException
import com.github.feilewu.monitor.core.rpc.{RpcEndpoint, RpcEndpointAddress, RpcEndpointRef}
import com.github.feilewu.monitor.network.client.RpcResponseCallback

private [rpc] final class Dispatcher(private val nettyEnv: NettyRpcEnv) {

  private var stopped = false

  def rpcEnv: NettyRpcEnv = nettyEnv

  private val endpoints: ConcurrentMap[String, MessageLoop] =
    new ConcurrentHashMap[String, MessageLoop]
  private val endpointRefs: ConcurrentMap[RpcEndpoint, RpcEndpointRef] =
    new ConcurrentHashMap[RpcEndpoint, RpcEndpointRef]

  private lazy val sharedLoop = new SharedMessageLoop(this, 5)

  def registerEndpoint(name: String, endpoint: RpcEndpoint): NettyRpcEndpointRef = {
    val addr = RpcEndpointAddress(nettyEnv.address, name)
    val endpointRef = new NettyRpcEndpointRef(nettyEnv, addr)
    if (stopped) {
      throw new IllegalStateException("RpcEnv has been stopped")
    }
    if (endpoints.get(name) != null) {
      throw new IllegalArgumentException(s"There is already an RpcEndpoint called $name")
    }

    endpointRefs.put(endpoint, endpointRef)
    var messageLoop: MessageLoop = null
    try {
      messageLoop = endpoint match {
        case _ =>
          sharedLoop.register(name, endpoint)
          sharedLoop
      }
      endpoints.put(name, messageLoop)
    } catch {
      case NonFatal(e) =>
        endpointRefs.remove(endpoint)
        throw e
    }
    endpointRef
  }

  def removeRpcEndpointRef(endpoint: RpcEndpoint): Unit = endpointRefs.remove(endpoint)

  def getRpcEndpointRef(endpoint: RpcEndpoint): RpcEndpointRef = endpointRefs.get(endpoint)

  /** Posts a message sent by a local endpoint. */
  def postLocalMessage(message: RequestMessage, p: Promise[Any]): Unit = {
    val rpcCallContext =
      new LocalNettyRpcCallContext(message.senderAddress, p)
    val rpcMessage = RpcMessage(message.senderAddress, message.content, rpcCallContext)
    postMessage(message.receiver.name, rpcMessage, (e) => p.tryFailure(e))
  }

  /** Posts a one-way message. */
  def postOneWayMessage(message: RequestMessage): Unit = {
    postMessage(message.receiver.name, OneWayMessage(message.senderAddress, message.content),
      {
        case e => throw e
      })
  }

  /** Posts a message sent by a remote endpoint. */
  def postRemoteMessage(message: RequestMessage, callback: RpcResponseCallback): Unit = {
    val rpcCallContext =
      new RemoteNettyRpcCallContext(nettyEnv, callback, message.senderAddress)
    val rpcMessage = RpcMessage(message.senderAddress, message.content, rpcCallContext)
    postMessage(message.receiver.name, rpcMessage, (e) => callback.onFailure(e))
  }

  private def postMessage(
                           endpointName: String,
                           message: InboxMessage,
                           callbackIfStopped: (Exception) => Unit): Unit = {
    val error = synchronized {
      val loop = endpoints.get(endpointName)
      if (stopped) {
        Some(new MonitorException(s"Dispatcher is stopped"))
      } else if (loop == null) {
        Some(new MonitorException(s"Could not find $endpointName."))
      } else {
        loop.post(endpointName, message)
        None
      }
    }
    // We don't need to call `onStop` in the `synchronized` block
    error.foreach(callbackIfStopped)
  }


  /**
   * Return if the endpoint exists
   */
  def verify(name: String): Boolean = {
    endpoints.containsKey(name)
  }

  private def unregisterRpcEndpoint(name: String): Unit = {
    val loop = endpoints.remove(name)
    if (loop != null) {
      loop.unregister(name)
    }
    // Don't clean `endpointRefs` here because it's possible that some messages are being processed
    // now and they can use `getRpcEndpointRef`. So `endpointRefs` will be cleaned in Inbox via
    // `removeRpcEndpointRef`.
  }

  def stop(): Unit = {
    synchronized {
      if (stopped) {
        return
      }
      stopped = true
    }

    var stopSharedLoop = false
    endpoints.asScala.foreach { case (name, loop) =>
      unregisterRpcEndpoint(name)
      if (!loop.isInstanceOf[SharedMessageLoop]) {
        loop.stop()
      } else {
        stopSharedLoop = true
      }
    }
    if (stopSharedLoop) {
      sharedLoop.stop()
    }
  }

}
