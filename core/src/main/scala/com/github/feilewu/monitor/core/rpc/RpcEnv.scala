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
 * @Date: 2024/4/14 0:35
 * @email：pfxuchn@gmail.com
 */

package com.github.feilewu.monitor.core.rpc

import java.util.concurrent.TimeUnit

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, MILLISECONDS}

import com.github.feilewu.monitor.core.ThreadUtils
import com.github.feilewu.monitor.core.conf.MonitorConf
import com.github.feilewu.monitor.core.conf.config.Network

private [monitor] abstract class RpcEnv {

  def conf: MonitorConf

  def address: RpcAddress

  def shutdown(): Unit
  /**
   * Return RpcEndpointRef of the registered [[RpcEndpoint]]. Will be used to implement
   * [[RpcEndpoint.self]]. Return `null` if the corresponding [[RpcEndpointRef]] does not exist.
   */
  private[rpc] def endpointRef(endpoint: RpcEndpoint): RpcEndpointRef

  def setupEndpoint(name: String, endpoint: RpcEndpoint): RpcEndpointRef

  def asyncSetupEndpointRefByURI(uri: String): Future[RpcEndpointRef]

  def setupEndpointRefByURI(uri: String): RpcEndpointRef = {
    val timeoutInSeconds = conf.get(Network.MONITOR_NETWORK_RPC_CONNECT_TIMEOUT)
    ThreadUtils.awaitResult(asyncSetupEndpointRefByURI(uri),
      Duration(timeoutInSeconds, TimeUnit.SECONDS))
  }

  def setupEndpointRef(address: RpcAddress, endpointName: String): RpcEndpointRef = {
    setupEndpointRefByURI(RpcEndpointAddress(address, endpointName).toString)
  }


}


