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
 * @Date: 2024/4/27 0:13
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.rpc.netty

import com.github.feilewu.monitor.core.conf.MonitorConf
import com.github.feilewu.monitor.core.rpc.{RpcAddress, RpcCallContext, RpcEndpoint, RpcEnv}
import org.scalatest.funsuite.AnyFunSuite

case class SayRequest(msg: String)

case class SayResponse(msg: String)

class NettyRpcEnvTest extends AnyFunSuite {

  private class DemoEndpoint(val rpcEnv: RpcEnv) extends RpcEndpoint {

    /**
     * Process messages from `RpcEndpointRef.ask`. If receiving a unmatched message,
     * `SparkException` will be thrown and sent to `onError`.
     */
    override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
      case SayRequest(msg) =>
        context.reply(SayResponse(s"receive: $msg"))
    }
  }


  test("test NettyRpcEnv create") {

    val conf = new MonitorConf
    val env = NettyRpcEnv.createNettyRpcEnv("localhost", conf)
    env.startServer(10020)
    env.setupEndpoint("demoEndpoint", new DemoEndpoint(env))
    val demoEndpointRef = env.setupEndpointRef(RpcAddress("localhost", 10020), "demoEndpoint")
    val response = demoEndpointRef.askSync[SayResponse](SayRequest("Hello, server!"))
    assert(response.msg != null && response.msg.contains("Hello, server!"))
    env.shutdown()
    
  }





}
