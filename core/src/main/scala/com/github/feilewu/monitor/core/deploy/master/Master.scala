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
 * @Date: 2024/4/27 18:47
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.deploy.master

import java.util

import com.github.feilewu.monitor.core.conf.MonitorConf
import com.github.feilewu.monitor.core.deploy.RegisterAgent
import com.github.feilewu.monitor.core.deploy.agent.AgentInfo
import com.github.feilewu.monitor.core.log.Logging
import com.github.feilewu.monitor.core.rpc.{RpcAddress, RpcCallContext, RpcEndpoint, RpcEnv}
import com.github.feilewu.monitor.core.rpc.netty.NettyRpcEnv

private[deploy] class Master(val rpcEnv: RpcEnv) extends RpcEndpoint with Logging {

  private val addressToAgent = new util.HashMap[RpcAddress, AgentInfo]()

  /**
   * Process messages from `RpcEndpointRef.ask`. If receiving a unmatched message,
   * `SparkException` will be thrown and sent to `onError`.
   */
  override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
    case RegisterAgent(cores, memory, agentRef) =>
      val address = agentRef.address
      val agentInfo = new AgentInfo("", address.host, address.port, cores, memory, agentRef)
      addressToAgent.put(address, agentInfo)
      logInfo(s"registered agent: ${address}")
      context.reply(true)
  }


  /**
   * Process messages from `RpcEndpointRef.send` or `RpcCallContext.reply`. If receiving a
   * unmatched message, `MonitorException` will be thrown and sent to `onError`.
   */
  override def receive: PartialFunction[Any, Unit] = {
    case OnStart => onStart()

  }


}

private[master] object OnStart

private[monitor] object Master {
  val NAME = "master"

  def main(args: Array[String]): Unit = {
    val conf = new MonitorConf
    val env = NettyRpcEnv.createNettyRpcEnv("localhost", conf)
    env.startServer(7077)
    env.setupEndpoint(Master.NAME, new Master(env))
    env.awaitTermination()
  }


}
