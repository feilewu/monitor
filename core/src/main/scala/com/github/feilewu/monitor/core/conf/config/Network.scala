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
 * @Date: 2024/4/27 8:57
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.conf.config

import java.util.concurrent.TimeUnit

private[monitor] object Network {


  private[monitor] val NETWORK_TIMEOUT =
    ConfigBuilder("monitor.network.timeout")
      .timeConf(TimeUnit.SECONDS)
      .createWithDefaultString("60s")

  private[monitor] val MONITOR_NETWORK_RPC_CONNECT_TIMEOUT =
    ConfigBuilder("monitor.network.rpc.connect.timeout")
      .timeConf(TimeUnit.SECONDS)
      .createWithDefaultString("10s")

  private[monitor] val MONITOR_NETWORK_RPC_AWAIT_TIMEOUT =
    ConfigBuilder("monitor.network.rpc.await.timeout")
      .timeConf(TimeUnit.SECONDS)
      .createWithDefaultString("30s")


}
