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
 * @Date: 2024/5/26 22:12
 * @email：pfxuchn@gmail.com
 */

package com.github.feilewu.monitor.core.deploy.master

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuite

import com.github.feilewu.monitor.core.conf.MonitorConf
import com.github.feilewu.monitor.core.conf.config.Config.MASTER_UI_SERVER_CLASS
import com.github.feilewu.monitor.core.rpc.RpcEnv
import com.github.feilewu.monitor.core.rpc.netty.NettyRpcEnv
import com.github.feilewu.monitor.core.ui.UIServer


private class TestUIServer extends UIServer {

  override def init(action: MasterAction): Unit = {

  }

  override def start(): Unit = {

  }

  override def stop(): Unit = {

  }
}

class MasterTest extends AnyFunSuite with BeforeAndAfterEach with BeforeAndAfterAll {

  private val conf = new MonitorConf

  private var env: RpcEnv = null

  override protected def beforeAll(): Unit = {
    env = NettyRpcEnv.createNettyRpcEnv("localhost", conf)
  }

  override protected def afterAll(): Unit = {
    env.shutdown()
  }

  test("startUiServer") {
    val master = new Master(env)
    assertThrows[ClassNotFoundException] {
      master.startUiServer()
    }

    conf.set(MASTER_UI_SERVER_CLASS.key,
      "com.github.feilewu.monitor.core.deploy.master.TestUIServer")
    master.startUiServer()

  }


}
