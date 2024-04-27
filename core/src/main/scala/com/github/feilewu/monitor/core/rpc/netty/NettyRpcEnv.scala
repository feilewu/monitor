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
 * @Date: 2024/4/20 21:26
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.rpc.netty

import java.io.{DataInputStream, DataOutputStream, OutputStream}
import java.nio.ByteBuffer
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap, TimeUnit}
import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.{Future, Promise, TimeoutException}
import scala.reflect.ClassTag
import scala.util.Try

import com.github.feilewu.monitor.core.ThreadUtils
import com.github.feilewu.monitor.core.conf.MonitorConf
import com.github.feilewu.monitor.core.rpc._
import com.github.feilewu.monitor.core.serializer._
import com.github.feilewu.monitor.network.TransportContext
import com.github.feilewu.monitor.network.client.TransportClient
import com.github.feilewu.monitor.network.server.TransportServer

private [netty] class NettyRpcEnv(val host: String, val conf: MonitorConf)
  extends RpcEnv {

  private val javaSerializerInstance: JavaSerializerInstance =
    new JavaSerializer().newInstance().asInstanceOf[JavaSerializerInstance]

  private var server: TransportServer = _

  private val dispatcher: Dispatcher = new Dispatcher(this)

  private val context: TransportContext = new TransportContext(new NettyRpcHandler(dispatcher))

  private val outboxes: ConcurrentMap[RpcAddress, Outbox] =
    new ConcurrentHashMap[RpcAddress, Outbox]()

  private val timeoutScheduler =
    ThreadUtils.newDaemonSingleThreadScheduledExecutor("netty-rpc-env-timeout")

  private[netty] val clientConnectionExecutor = ThreadUtils.newDaemonCachedThreadPool(
    "netty-rpc-connection", 64)

  private val stopped = new AtomicBoolean(false)

  override lazy val address: RpcAddress = {
    if (server != null) {
      RpcAddress(host = host, port = server.getPort)
    } else {
      null
    }
  }

  def startServer(port: Int): Unit = {
    server = context.createTransportServer()
    server.bind(host, port);
    dispatcher.registerEndpoint(RpcEndpointVerifier.NAME, new RpcEndpointVerifier(this, dispatcher))
  }

  override private[rpc] def endpointRef(endpoint: RpcEndpoint): RpcEndpointRef = {
    null
  }

  override def setupEndpoint(name: String, endpoint: RpcEndpoint): RpcEndpointRef = {
    dispatcher.registerEndpoint(name, endpoint)
  }

  override def asyncSetupEndpointRefByURI(uri: String): Future[RpcEndpointRef] = {
    val address = RpcEndpointAddress(uri)
    val endpointRef = new NettyRpcEndpointRef(this, address)
    val verifier = new NettyRpcEndpointRef(this,
      RpcEndpointAddress(address.rpcAddress, RpcEndpointVerifier.NAME))
    verifier.ask[Boolean](RpcEndpointVerifier.CheckExistence(endpointRef.name)).flatMap { find =>
      if (find) {
        Future.successful(endpointRef)
      } else {
        Future.failed(new RpcEndpointNotFoundException(uri))
      }
    }(ThreadUtils.sameThread)
  }

  protected[netty] def ask[T: ClassTag](message: RequestMessage, timeout: RpcTimeout): Future[T] = {
    val promise = Promise[Any]()
    val remoteAddr = message.receiver.address
    var rpcMsg: Option[RpcOutboxMessage] = None
    def onFailure(e: Throwable): Unit = {
      if (!promise.tryFailure(e)) {
        e match {
          case _ =>
        }
      }
    }
    def onSuccess(reply: Any): Unit = reply match {
      case RpcFailure(e) => onFailure(e)
      case rpcReply =>
        if (!promise.trySuccess(rpcReply)) {

        }
    }
    val rpcMessage = RpcOutboxMessage(message.serialize(this),
      onFailure,
      (client, response) => onSuccess(deserialize[Any](response)))
    rpcMsg = Option(rpcMessage)
    postToOutbox(message.receiver, rpcMessage)
    promise.future.failed.foreach {
      case _: TimeoutException =>
      case _ =>
    }(ThreadUtils.sameThread)
    val timeoutCancelable = timeoutScheduler.schedule(new Runnable {
      override def run(): Unit = {
        val remoteRecAddr = if (remoteAddr == null) {
          Try {
            message.receiver.client.getChannel.remoteAddress()
          }.toOption.orNull
        } else {
          remoteAddr
        }
        onFailure(new TimeoutException(s"Cannot receive any reply from ${remoteRecAddr} " +
          s"in ${timeout.duration}"))
      }
    }, timeout.duration.toNanos, TimeUnit.NANOSECONDS)
    promise.future.onComplete { v =>
      timeoutCancelable.cancel(true)
    }(ThreadUtils.sameThread)
    promise.future.mapTo[T]
  }

  private[netty] def serialize(content: Any): ByteBuffer = {
    javaSerializerInstance.serialize(content)
  }

  /**
   * Returns [[SerializationStream]] that forwards the serialized bytes to `out`.
   */
  private[netty] def serializeStream(out: OutputStream): SerializationStream = {
    javaSerializerInstance.serializeStream(out)
  }

  private[netty] def deserialize[T: ClassTag](bytes: ByteBuffer): T = {
    javaSerializerInstance.deserialize[T](bytes)
  }

  private[netty] def createClient(address: RpcAddress): TransportClient = {
    context.createClient(address.host, address.port)
  }

  private[rpc] def postToOutbox(receiver: NettyRpcEndpointRef,
                                message: OutboxMessage): Unit = {

    require(receiver.address != null,
      "Cannot send message to client endpoint with no listen address.")
    val targetOutbox = {
      val outbox = outboxes.get(receiver.address)
      if (outbox == null) {
        val newOutbox = new Outbox(this, receiver.address)
        val oldOutbox = outboxes.putIfAbsent(receiver.address, newOutbox)
        if (oldOutbox == null) {
          newOutbox
        } else {
          oldOutbox
        }
      } else {
        outbox
      }
    }
    targetOutbox.send(message)
  }

  private def cleanup(): Unit = {
    if (!stopped.compareAndSet(false, true)) {
      return
    }
    val iter = outboxes.values().iterator()
    while (iter.hasNext()) {
      val outbox = iter.next()
      outboxes.remove(outbox.rpcAddress)
      outbox.stop()
    }
    if (timeoutScheduler != null) {
      timeoutScheduler.shutdownNow()
    }
    if (dispatcher != null) {
      dispatcher.stop()
    }
    if (server != null) {
      server.close()
    }
    if (clientConnectionExecutor != null) {
      clientConnectionExecutor.shutdownNow()
    }
    if (context != null) {
      context.close()
    }
  }

  override def shutdown(): Unit = cleanup()
}

private[monitor] object NettyRpcEnv {

  def createNettyRpcEnv(host: String, conf: MonitorConf): NettyRpcEnv = {
    new NettyRpcEnv(host, conf)
  }

}

private[netty] case class RpcFailure(e: Throwable)

private[netty] class RequestMessage(val senderAddress: RpcAddress,
                                  val receiver: NettyRpcEndpointRef,
                                  val content: Any) {

  def serialize(nettyEnv: NettyRpcEnv): ByteBuffer = {
    val bos = new ByteBufferOutputStream()
    val out = new DataOutputStream(bos)
    try {
      writeRpcAddress(out, senderAddress)
      writeRpcAddress(out, receiver.address)
      out.writeUTF(receiver.name)
      val s = nettyEnv.serializeStream(out)
      try {
        s.writeObject(content)
      } finally {
        s.close()
      }
    } finally {
      out.close()
    }
    bos.toByteBuffer
  }

  private def writeRpcAddress(out: DataOutputStream, rpcAddress: RpcAddress): Unit = {
    if (rpcAddress == null) {
      out.writeBoolean(false)
    } else {
      out.writeBoolean(true)
      out.writeUTF(rpcAddress.host)
      out.writeInt(rpcAddress.port)
    }
  }

}

private[netty] object RequestMessage {

  private def readRpcAddress(in: DataInputStream): RpcAddress = {
    val hasRpcAddress = in.readBoolean()
    if (hasRpcAddress) {
      RpcAddress(in.readUTF(), in.readInt())
    } else {
      null
    }
  }

  def apply(nettyEnv: NettyRpcEnv, client: TransportClient, bytes: ByteBuffer): RequestMessage = {
    val bis = new ByteBufferInputStream(bytes)
    val in = new DataInputStream(bis)
    try {
      val senderAddress = readRpcAddress(in)
      val endpointAddress = RpcEndpointAddress(readRpcAddress(in), in.readUTF())
      val ref = new NettyRpcEndpointRef(nettyEnv, endpointAddress)
      ref.client = client
      new RequestMessage(
        senderAddress,
        ref,
        // The remaining bytes in `bytes` are the message content.
        nettyEnv.deserialize(bytes))
    } finally {
      in.close()
    }
  }
}

