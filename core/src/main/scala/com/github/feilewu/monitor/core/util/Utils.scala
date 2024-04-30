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
 * @Date: 2024/4/29 20:51
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.util

import scala.util.control.NonFatal

import com.github.feilewu.monitor.core.log.Logging
import com.github.feilewu.monitor.core.rpc.RpcAddress

private[monitor] object Utils {


  def parseMasterUrls(masterUrls: String): Seq[RpcAddress] = {
    if (masterUrls == null || !masterUrls.startsWith("monitor://")) {
      throw new IllegalArgumentException(s"Illegal masterUrls: $masterUrls")
    }

    val hostPorts = masterUrls.replace("monitor://", "")
      .split(",")
      .filter(s => s.nonEmpty)
      .map(s => {
        val hostAndPort = s.split(":")
        (hostAndPort(0), hostAndPort(1))
      })

    hostPorts.map(tuple => RpcAddress(tuple._1, tuple._2.toInt))
  }


  def tryLogNonFatal(logging: Logging)(block: => Any): Unit = {
    try {
      block
    } catch {
      case NonFatal(e) => logging.logInfo(s"Catch exception: ${e.getMessage}", e)
    }
  }


}
