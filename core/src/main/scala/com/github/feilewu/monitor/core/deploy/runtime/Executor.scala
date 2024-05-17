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
 * @Date: 2024/5/6 22:04
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.deploy.runtime

import java.io.{File, FileOutputStream, InputStream}
import java.util.UUID

import scala.concurrent.Promise

import com.github.feilewu.monitor.core.deploy.runtime.Executor.{random, writeToFile}
import com.github.feilewu.monitor.core.log.Logging
import com.github.feilewu.monitor.core.util.IOUtils

private[monitor] trait ExecutorCallback {

 def onSuccess(): Unit = {

 }

}



private[runtime] abstract class Executor(val logEnabled: Boolean,
                                        val logDir: String) extends Logging {

  private var process: Process = null

  private val executorId: String = random

  final def id: String = executorId

  private def build: ProcessBuilder = {
    import scala.jdk.CollectionConverters._
    val builder = new ProcessBuilder()
    builder.command(command.asJava)
  }

  def command: List[String]

  final def run(): Unit = {

  }

  final def run(p: Promise[Any]): Unit = {
    try {
      logInfo(s"Begin to run command: $executorId")
      process = build.start()
      if (logEnabled) {
        logStdout(process.getInputStream, s"$logDir/$executorId")
        logStderr(process.getErrorStream, s"$logDir/$executorId")
      }
      val exitCode = process.waitFor()
      logInfo(s"Command:$executorId finished with code $exitCode")
      p.success(executorId)
    } catch {
      case throwable: Throwable =>
        p.failure(ExecuteException(executorId, throwable))
    }
  }

  protected def logStdout(inputStream: InputStream, path: String) = {
    val logFile = new File(path + File.separator + "stdout")
    writeToFile(inputStream, logFile)
  }

  protected def logStderr(inputStream: InputStream, path: String) = {
    val logFile = new File(path + File.separator + "stderr")
    writeToFile(inputStream, logFile)
  }

}

class V2rayExecutor(override val logEnabled: Boolean,
                    override val logDir: String) extends Executor(logEnabled, logDir) {


  override def command: List[String] = {
    List.newBuilder
      .addOne("v2ray")
      .addOne("url")
      .result()
  }
}


case class ExecuteException(commandId: String, throwable: Throwable) extends IllegalStateException


object Executor {

  def random: String = {
    UUID.randomUUID().toString
  }

  def writeToFile(inputStream: InputStream, file: File): Thread = {
    IOUtils.createFile(file.getPath)
    var shouldStop = false
    val thread = new Thread(() => {
      val outputStream = new FileOutputStream(file)
      try {
        val b = new Array[Byte](1024)
        while (!shouldStop) {
          val len = inputStream.read(b)
          if (len <= 0) {
            shouldStop = true
          } else {
            outputStream.write(b, 0, len)
          }
        }
      } finally {
        outputStream.close()
      }
    })
    thread.start()
    thread
  }

}
