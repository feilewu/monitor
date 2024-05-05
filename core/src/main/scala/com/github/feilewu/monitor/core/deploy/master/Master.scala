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
import java.util.concurrent.TimeUnit

import com.github.feilewu.monitor.core.ThreadUtils
import com.github.feilewu.monitor.core.conf.MonitorConf
import com.github.feilewu.monitor.core.deploy.{HeartBeat, RegisterAgent}
import com.github.feilewu.monitor.core.deploy.agent.AgentInfo
import com.github.feilewu.monitor.core.log.Logging
import com.github.feilewu.monitor.core.rpc.{RpcAddress, RpcCallContext, RpcEndpoint, RpcEnv}
import com.github.feilewu.monitor.core.rpc.netty.NettyRpcEnv
import com.github.feilewu.monitor.core.util.Utils

private[deploy] class Master(val rpcEnv: RpcEnv) extends RpcEndpoint with Logging {

  private val lastHeartBeatOfAgent = new util.HashMap[RpcAddress, Long]()

  private val agentHeartBeat = ThreadUtils
    .newDaemonSingleThreadScheduledExecutor("heart-beat-thread")

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
      lastHeartBeatOfAgent.put(address, System.currentTimeMillis())
      logInfo(s"registered agent: ${address}")
      context.reply(true)
  }


  /**
   * Process messages from `RpcEndpointRef.send` or `RpcCallContext.reply`. If receiving a
   * unmatched message, `MonitorException` will be thrown and sent to `onError`.
   */
  override def receive: PartialFunction[Any, Unit] = {
    case OnStart => onStart()
    case HeartBeat(rpcAddress) =>
      val lastTime: Long = lastHeartBeatOfAgent.get(rpcAddress)
      Utils.require(lastTime != 0)
      lastHeartBeatOfAgent.put(rpcAddress, System.currentTimeMillis())
  }


  override def onStart(): Unit = {
    agentHeartBeat.scheduleAtFixedRate(() => {
      Utils.tryLogNonFatal({
        logInfo("Start checking whether the agent is alive.")
        lastHeartBeatOfAgent.synchronized {
          lastHeartBeatOfAgent.forEach((address, time) => {
            if (System.currentTimeMillis() - time > 1 * 60 * 1000) {
              lastHeartBeatOfAgent.remove(address)
              addressToAgent.remove(address)
              logInfo(s"Agent ${address} has been removed from master!")
            }
          })
        }
      })
    }, 0, 60, TimeUnit.SECONDS)
  }
}

private[master] object OnStart

private[monitor] object Master {
  val NAME = "master"

  def main(args: Array[String]): Unit = {
    val conf = new MonitorConf
    val env = NettyRpcEnv.createNettyRpcEnv("localhost", conf)
    env.startServer(7077)
    val endpointRef = env.setupEndpoint(Master.NAME, new Master(env))
    endpointRef.send(OnStart)
    env.awaitTermination()
  }


}
