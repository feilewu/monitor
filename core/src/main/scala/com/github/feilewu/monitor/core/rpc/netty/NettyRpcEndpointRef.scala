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
 * @Date: 2024/4/21 10:12
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.rpc.netty

import scala.concurrent.Future
import scala.reflect.ClassTag

import com.github.feilewu.monitor.core.rpc.{RpcAddress, RpcEndpointAddress, RpcEndpointRef, RpcEnv, RpcTimeout}
import com.github.feilewu.monitor.network.client.TransportClient

private [rpc] class NettyRpcEndpointRef(private val nettyEnv: NettyRpcEnv,
                                        private val endpointAddress: RpcEndpointAddress)
  extends RpcEndpointRef {

  var client: TransportClient = _

  override def name: String = endpointAddress.name

  override def address: RpcAddress = {
    if (endpointAddress.rpcAddress != null ) endpointAddress.rpcAddress else null
  }


  override def ask[T: ClassTag](message: Any, timeout: RpcTimeout): Future[T] = {
    nettyEnv.ask(new RequestMessage(nettyEnv.address, this, message), timeout)
  }

  override def rpcEnv: RpcEnv = nettyEnv

  override def send(message: Any): Unit = {
    nettyEnv.send(new RequestMessage(nettyEnv.address, this, message))
  }
}
