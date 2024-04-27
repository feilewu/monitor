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
 * @Date: 2024/4/14 18:37
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.rpc

import com.github.feilewu.monitor.core.exception.MonitorException

case class RpcEndpointAddress(rpcAddress: RpcAddress, name: String) {

  def this(host: String, port: Int, name: String) = {
    this(RpcAddress(host, port), name)
  }

  override val toString: String = if (rpcAddress != null) {
    s"monitor://$name@${rpcAddress.host}:${rpcAddress.port}"
  } else {
    s"monitor-client://$name"
  }

}

private[monitor] object RpcEndpointAddress {

  def apply(host: String, port: Int, name: String): RpcEndpointAddress = {
    new RpcEndpointAddress(host, port, name)
  }

  def apply(monitorUrl: String): RpcEndpointAddress = {
    try {
      val uri = new java.net.URI(monitorUrl)
      val host = uri.getHost
      val port = uri.getPort
      val name = uri.getUserInfo
      if (uri.getScheme != "monitor" ||
        host == null ||
        port < 0 ||
        name == null ||
        (uri.getPath != null && !uri.getPath.isEmpty) || // uri.getPath returns "" instead of null
        uri.getFragment != null ||
        uri.getQuery != null) {
        throw new MonitorException("Invalid Monitor URL: " + monitorUrl)
      }
      new RpcEndpointAddress(host, port, name)
    } catch {
      case e: java.net.URISyntaxException =>
        throw new MonitorException("Invalid Monitor URL: " + monitorUrl, e)
    }
  }
}
