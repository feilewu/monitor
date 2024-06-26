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

import scala.collection.mutable
import scala.jdk.CollectionConverters.MapHasAsScala

import com.github.feilewu.monitor.core.ThreadUtils
import com.github.feilewu.monitor.core.conf.MonitorConf
import com.github.feilewu.monitor.core.conf.config.Config.{MASTER_UI_ENABLED, MASTER_UI_SERVER_CLASS}
import com.github.feilewu.monitor.core.conf.config.Network.NETWORK_TIMEOUT
import com.github.feilewu.monitor.core.deploy.{HeartBeat, RegisterAgent}
import com.github.feilewu.monitor.core.deploy.agent.AgentRegisterInfo
import com.github.feilewu.monitor.core.deploy.runtime.V2rayManager
import com.github.feilewu.monitor.core.log.Logging
import com.github.feilewu.monitor.core.rpc._
import com.github.feilewu.monitor.core.rpc.netty.NettyRpcEnv
import com.github.feilewu.monitor.core.ui.{AgentInfo, AgentInfoCollection, UIServer}
import com.github.feilewu.monitor.core.util.Utils

private[core] class Master(val rpcEnv: RpcEnv) extends RpcEndpoint
  with Logging
  with MasterAction {

  private val conf: MonitorConf = rpcEnv.conf

  private val lastHeartBeatOfAgent = new util.HashMap[RpcAddress, Long]()

  private val agentHeartBeat = ThreadUtils
    .newDaemonSingleThreadScheduledExecutor("heart-beat-thread")

  private val v2rayExecutor =
    ThreadUtils.newDaemonSingleThreadScheduledExecutor("v2ray-schedule-thread")

  private val addressToAgent = new util.HashMap[RpcAddress, AgentRegisterInfo]()

  private val agentRefs = new util.HashMap[RpcAddress, RpcEndpointRef]()

  private val v2rayManager = new V2rayManager(true, false, this)

  private var uiServer: UIServer = null

  private[monitor] def agents: List[RpcEndpointRef] =
    agentRefs.asScala.toList.map(tuple => tuple._2)

  /**
   * Process messages from `RpcEndpointRef.ask`. If receiving a unmatched message,
   * `SparkException` will be thrown and sent to `onError`.
   */
  override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
    case RegisterAgent(agentRef) =>
      val address = agentRef.address
      agentRefs.put(address, agentRef)
      val agentInfo = new AgentRegisterInfo("", address.host, address.port, agentRef)
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
      lastHeartBeatOfAgent.synchronized {
        if (lastHeartBeatOfAgent.isEmpty) {
          logWarning(s"lastHeartBeatOfAgent is empty, ignore heartbeat message")
        } else {
          val lastTime: Long = lastHeartBeatOfAgent.get(rpcAddress)
          Utils.require(lastTime != 0)
          lastHeartBeatOfAgent.put(rpcAddress, System.currentTimeMillis())
        }
      }
  }

  private[master] def startUiServer(): Unit = {
    if (conf.get(MASTER_UI_ENABLED)) {
      val uiServerClass: Class[UIServer] = Utils.classForName(conf.get(MASTER_UI_SERVER_CLASS))
      uiServer = uiServerClass.newInstance()
      uiServer.init(this)
      uiServer.start()
    }
  }

  override def onStart(): Unit = {
    try {
      startUiServer()

      agentHeartBeat.scheduleAtFixedRate(() => {
        Utils.tryLogNonFatal({
          logTrace("Start checking whether the agent is alive.")
          lastHeartBeatOfAgent.synchronized {
            val needRemove: mutable.ListBuffer[RpcAddress] = mutable.ListBuffer.empty
            lastHeartBeatOfAgent.forEach((address, time) => {
              if (System.currentTimeMillis() - time > conf.get(NETWORK_TIMEOUT) * 1000) {
                needRemove += address
              }
            })
            needRemove.foreach(address => {
              lastHeartBeatOfAgent.remove(address)
              addressToAgent.remove(address)
              logInfo(s"Agent ${address} has been removed from master!")
            })
          }
        })
      }, 5, 5, TimeUnit.SECONDS)

      v2rayExecutor.scheduleAtFixedRate(() => {
        sendV2rayExecutionToAllAgents()
      }, 5, 5, TimeUnit.SECONDS)
    } catch {
      case throwable: Throwable =>
        logError("Master started failed.", throwable)
        this.onStop()
        System.exit(-1)
    }

  }

  def sendV2rayExecutionToAgent(agent: RpcEndpointRef): Unit = {
    v2rayManager.sendExecution(agent)
  }

  def sendV2rayExecutionToAllAgents(): Unit = {
    agents.foreach(sendV2rayExecutionToAgent)
  }

  override def onStop(): Unit = {
    agentHeartBeat.shutdown()
    v2rayExecutor.shutdown()
    if (uiServer != null) {
      uiServer.stop()
    }
  }

  override def monitorConf: MonitorConf = conf

  override def agentInfos: AgentInfoCollection = {
    val agentInfoList = addressToAgent.asScala
      .map {
        case (address, _) =>
          AgentInfo(address.host, address.port.toString)
      }.toList
    new AgentInfoCollection(agentInfoList)
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
