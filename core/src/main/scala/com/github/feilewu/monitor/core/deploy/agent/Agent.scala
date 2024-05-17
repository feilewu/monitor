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
 * @Date: 2024/4/28 20:26
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.deploy.agent

import java.util
import java.util.concurrent.{ScheduledExecutorService, ThreadPoolExecutor, TimeUnit}

import scala.concurrent.{ExecutionContext, Promise}
import scala.util.{Failure, Success}

import com.github.feilewu.monitor.core.ThreadUtils
import com.github.feilewu.monitor.core.conf.MonitorConf
import com.github.feilewu.monitor.core.conf.config.Config
import com.github.feilewu.monitor.core.conf.config.Config.EXECUTOR_LOG_DIR
import com.github.feilewu.monitor.core.deploy.{ExecuteV2ry, HeartBeat, RegisterAgent}
import com.github.feilewu.monitor.core.deploy.master.Master
import com.github.feilewu.monitor.core.deploy.runtime.{ExecuteException, V2rayExecutor}
import com.github.feilewu.monitor.core.log.Logging
import com.github.feilewu.monitor.core.rpc._
import com.github.feilewu.monitor.core.rpc.netty.NettyRpcEnv
import com.github.feilewu.monitor.core.util.Utils

private[deploy] class Agent(val rpcEnv: RpcEnv)
  extends RpcEndpoint with Logging {

  private var masterRef: RpcEndpointRef = null

  private val agentExecutors: ThreadPoolExecutor =
    ThreadUtils.newDaemonFixedThreadPool(3, "agent-executor-thread")

  private val agentExecutorsExecutionContext =
    ExecutionContext.fromExecutorService(agentExecutors)

  private val heartBeatScheduledExecutorService: ScheduledExecutorService =
    ThreadUtils.newDaemonSingleThreadScheduledExecutor("heartbeat-thread")

  private val v2rayExecutorCommands = new util.HashSet[String]

  override def receive: PartialFunction[Any, Unit] = {
    case OnStart => onStart()
    case RegisterSelf =>
      val cores = Runtime.getRuntime.availableProcessors()
      val memory = "2048G"
      Utils.tryLogNonFatal(this) {
        val resp = masterRef.askSync[Boolean](RegisterAgent(cores.toString, memory, this.self))
        if (!resp) {
          logError(s"Cannot register itself in master, ${masterRef.address}")
          System.exit(-1)
        } else {
          logInfo(s"Register to master ${masterRef.address} successfully!")
          heartBeatScheduledExecutorService.scheduleAtFixedRate(() => {
            masterRef.send(HeartBeat(rpcEnv.address))
            logDebug(s"Send heatBeat message at ${System.currentTimeMillis()}")
          }, 0, 10, TimeUnit.SECONDS)
        }
      }

  }

  override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
    case ExecuteV2ry =>
      val logDir = rpcEnv.conf.get(EXECUTOR_LOG_DIR)
      def onSuccess(id: Any): Unit = {
        logInfo(s"command: $id executed successfully")
        context.reply(id)
      }
      def onFailure(e: Throwable): Unit = {
        e match {
          case ExecuteException(commandId, throwable) =>
            logError(s"command:$commandId executed failed.", throwable)
            context.reply()
        }
      }
      val p = Promise[Any]()
      p.future.onComplete {
        case Success(id) => onSuccess(id)
        case Failure(e) => onFailure(e)
      } (agentExecutorsExecutionContext)
      val v2rayExecutor: V2rayExecutor = new V2rayExecutor(true, logDir)
      agentExecutors.execute(() => {
        v2rayExecutor.run(p)
      })
      v2rayExecutorCommands.synchronized {
        v2rayExecutorCommands.add(v2rayExecutor.id)
      }
      context.reply(v2rayExecutor.id)
  }

  override def onStart(): Unit = {
    val masterUrls = rpcEnv.conf.get(Config.MONITOR_MASTER)
    connectToMasters(Utils.parseMasterUrls(masterUrls))
    this.self.send(RegisterSelf)
  }

  override def onStop(): Unit = {
    agentExecutors.shutdown()
    heartBeatScheduledExecutorService.shutdown()
  }

  private def connectToMasters(masterRpcAddress: Seq[RpcAddress]): Unit = {
    connect(masterRpcAddress.head)
    def connect(rpcAddress: RpcAddress): Unit = {
      var shouldStop = false
      var count = 0
      while (!shouldStop) {
        if (count >= 10) {
          logError("Can not connect to master!")
          System.exit(-1)
        }
        try {
          logInfo(s"Connecting to master for the $count time.")
          masterRef = rpcEnv.setupEndpointRef(rpcAddress, Master.NAME)
          logInfo(s"Connected to master ($masterRpcAddress) successfully!")
          shouldStop = true
        } catch {
          case e: Exception =>
            logInfo(s"Failed connecting to master for the $count time.", e)
            Thread.sleep(10 * 1000)
            count = count + 1
        }
      }
    }
  }
}
private[agent] object OnStart

case object RegisterSelf

private[monitor] object Agent {

  val NAME = "agent"

  def main(args: Array[String]): Unit = {
    val conf = new MonitorConf
    val env = NettyRpcEnv.createNettyRpcEnv("localhost", conf)
    env.startServer(9090)
    val agentRef = env.setupEndpoint(Agent.NAME, new Agent(env))
    agentRef.send(OnStart)
    env.awaitTermination()

  }


}
