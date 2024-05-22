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
 * @Date: 2024/5/7 21:56
 * @email：pfxuchn@gmail.com
 */

package com.github.feilewu.monitor.core.deploy.runtime

import java.io.{ByteArrayInputStream, File}
import java.nio.charset.StandardCharsets

import scala.io.Source

import org.scalatest.funsuite.AnyFunSuite

import com.github.feilewu.monitor.core.util.IOUtils

class ExecutorTest extends AnyFunSuite {

  private val root: String = Thread.currentThread().getContextClassLoader.getResource("").getPath

  test("writeToFile") {
    val inputStream = new ByteArrayInputStream("hello world".getBytes(StandardCharsets.UTF_8))
    val file = new File(s"${root}data${File.separator}write${File.separator}stdout")
    IOUtils.createFile(file.getPath)
    val thread = Executor.writeToFile(inputStream, file)
    thread.join(10000)
    val source = Source.fromFile(file.getPath, "UTF-8")
    try {
      assert(source.mkString.equals("hello world"))
    } finally {
      source.close()
      file.delete()
    }
  }

  test("regularMatchingV2ray") {

    import Executor._

    val message = "---------- V2Ray vmess URL / V2RayNG v0.4.1+ / V2RayN v2.1+ / " +
      "仅适合部分客户端 -------------\n\n" +
      "vmess://ewoidiI6ICIyIiwKInBzIjogIjIzM3YyLmNvbV83NC40OC43NS42MiIsCiJhZ" +
      "GQiOiAiNzQuNDguNzUuNjIiLAoicG9ydCI6ICIyOTg5OSIsCiJpZCI6ICJkNjA5MWIyNS1" +
      "lNmExLTRmZDQtYjA4MC02OGM4ZmRiZDkzNDIiLAoiYWlkIjogIjAiLAoibmV0IjogInRjcCI" +
      "sCiJ0eXBlIjogIm5vbmUiLAoiaG9zdCI6ICIiLAoicGF0aCI6ICIiLAoidGxzIjogIiIKfQo=\n\n" +
      "免被墙..推荐使用JMS: https://getjms.com"
    assert(regularMatchingV2ray(message).isDefined)
    val messageWithoutVmess = "hello"
    assert(regularMatchingV2ray(messageWithoutVmess).isEmpty)
  }


}
