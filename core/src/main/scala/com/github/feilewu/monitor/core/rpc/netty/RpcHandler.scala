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
 * @Date: 2024/4/20 22:51
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.rpc.netty

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import com.github.feilewu.monitor.core.rpc.RpcAddress
import com.github.feilewu.monitor.network.client.{RpcResponseCallback, TransportClient}
import com.github.feilewu.monitor.network.server.RpcHandler

private [rpc] class NettyRpcHandler(dispatcher: Dispatcher) extends RpcHandler {

  override def receive(client: TransportClient, message: ByteBuffer,
                       callback: RpcResponseCallback): Unit = {

    val requestMessage = internalReceive(client, message)

    dispatcher.postRemoteMessage(requestMessage, callback)


  }

  private def internalReceive(client: TransportClient, message: ByteBuffer): RequestMessage = {
    val addr = client.getChannel().remoteAddress().asInstanceOf[InetSocketAddress]
    assert(addr != null)
    val clientAddr = RpcAddress(addr.getHostString, addr.getPort)
    val requestMessage = RequestMessage(dispatcher.rpcEnv, client, message)
    if (requestMessage.senderAddress == null) {
      // Create a new message with the socket address of the client as the sender.
      new RequestMessage(clientAddr, requestMessage.receiver, requestMessage.content)
    } else {
      // The remote RpcEnv listens to some port, we should also fire a RemoteProcessConnected for
      // the listening address
      //      val remoteEnvAddress = requestMessage.senderAddress
      //      if (remoteAddresses.putIfAbsent(clientAddr, remoteEnvAddress) == null) {
      //        dispatcher.postToAll(RemoteProcessConnected(remoteEnvAddress))
      //      }
      requestMessage
    }
  }


}
