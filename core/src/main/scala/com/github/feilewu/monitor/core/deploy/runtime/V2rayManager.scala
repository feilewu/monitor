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
 * @Date: 2024/5/17 22:35
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.deploy.runtime

import java.io.Closeable
import java.util
import java.util.concurrent.{ConcurrentHashMap, ScheduledExecutorService, ThreadPoolExecutor, TimeUnit}

import scala.concurrent.Promise
import scala.io.{BufferedSource, Source}
import scala.util.{Failure, Success}

import com.github.feilewu.monitor.core.ThreadUtils
import com.github.feilewu.monitor.core.deploy.{ExecuteTaskState, ExecuteV2ry, PollTaskState, V2rayFailure, V2rayRunning, V2raySuccess}
import com.github.feilewu.monitor.core.deploy.agent.Agent
import com.github.feilewu.monitor.core.log.Logging
import com.github.feilewu.monitor.core.rpc.{RpcEndpoint, RpcEndpointRef}
import com.github.feilewu.monitor.core.util.Utils

private[deploy] class V2rayManager(val isMaster: Boolean, val isAgent: Boolean,
                                   val endPoint: RpcEndpoint) extends Logging with Closeable{

  require(isMaster ^ isAgent,
    s"isMaster: $isMaster and isAgent: $isAgent must not be true or false at the same time.")

  private var agentExecutor: ThreadPoolExecutor = null

  private var v2rayPollResultService: ScheduledExecutorService = null

  private var commandIdToAgent: ConcurrentHashMap[String, RpcEndpointRef] = null

  private var agentToV2rayUrl: ConcurrentHashMap[RpcEndpointRef, String] = null

  if (isMaster) {
    commandIdToAgent = new ConcurrentHashMap[String, RpcEndpointRef]()
    agentToV2rayUrl = new ConcurrentHashMap[RpcEndpointRef, String]()
    v2rayPollResultService = ThreadUtils
      .newDaemonSingleThreadScheduledExecutor("v2ray-execute-result-thread")
    v2rayPollResultService.scheduleAtFixedRate(() => {
      Utils.tryLogNonFatal({
        require(commandIdToAgent != null, "commandIdToAgent must be initialized")
        val iter = commandIdToAgent.entrySet().iterator()
        while (iter.hasNext) {
          val entry = iter.next()
          val (id, agent) = (entry.getKey, entry.getValue)
          val result = agent.askSync[ExecuteTaskState](PollTaskState(id))
          result match {
            case V2rayRunning() =>
            case V2raySuccess(v2rayUrl) =>
              agentToV2rayUrl.put(agent, v2rayUrl)
              iter.remove()
            case V2rayFailure(msg) =>
              logError(s"Command: $id execute failed, err: $msg")
              iter.remove()
          }
        }
      })
    }, 10, 10, TimeUnit.SECONDS)
  }

  private var commandIds: util.HashSet[String] = null

  if (isAgent) {
    commandIds = new util.HashSet[String]()
    agentExecutor = ThreadUtils
      .newDaemonFixedThreadPool(1, "v2ray-agent-executor-thread")
  }

  def sendExecution(agentRef: RpcEndpointRef): Unit = {
    val commandId = agentRef.askSync[String](ExecuteV2ry)
    commandIdToAgent.put(commandId, agentRef)
    logInfo(s"v2ray command has been sent to agent, id: $commandId")
  }

  def executeV2ry(logDir: String): String = {

    def onSuccess(id: Any): Unit = {
      logInfo(s"command: $id executed successfully")
    }
    def onFailure(e: Throwable): Unit = {
      e match {
        case ExecuteException(commandId, throwable) =>
          logError(s"command:$commandId executed failed.", throwable)
          commandIds.synchronized {
            commandIds.remove(commandId)
          }
      }
    }
    val p = Promise[Any]()
    p.future.onComplete {
      case Success(id) => onSuccess(id)
      case Failure(e) => onFailure(e)
    } (endPoint.asInstanceOf[Agent].executionContext)

    val v2rayExecutor: V2rayExecutor = new V2rayExecutor(true, logDir)
    agentExecutor.execute(() => {
      v2rayExecutor.run(p)
    })
    commandIds.synchronized {
      commandIds.add(v2rayExecutor.id)
    }
    v2rayExecutor.id
  }


  def readTaskResult(id: String, logDir: String): ExecuteTaskState = {
    val logFilePath = s"$logDir/$id/stdout"
    var source: BufferedSource = null
    try {
      source = Source.fromFile(logFilePath, "UTF-8")
      val stdout = source.mkString
      val optionVmess = Executor.regularMatchingV2ray(stdout)
      if (optionVmess.isDefined) {
        V2raySuccess(optionVmess.get)
      } else {
        V2rayFailure("Can not found vmess url in stdout.")
      }
    }
    catch {
        case throwable: Throwable => V2rayFailure(s"exception: ${throwable.toString}")
    }
    finally {
      if (source != null) {
        source.close()
      }
    }
  }

  override def close(): Unit = {
    if (agentExecutor != null) {
      agentExecutor.shutdown()
    }

    if (v2rayPollResultService != null) {
      v2rayPollResultService.shutdown()
    }

  }
}
