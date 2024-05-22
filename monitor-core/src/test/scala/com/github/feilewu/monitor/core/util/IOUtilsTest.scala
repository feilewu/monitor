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
 * @Date: 2024/5/7 22:18
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.util

import java.io.File

import org.scalatest.funsuite.AnyFunSuite

class IOUtilsTest extends AnyFunSuite {

  test("createFileRecursively") {
    val root = Thread.currentThread().getContextClassLoader.getResource("").getPath
    val path = s"$root/data/test/testFile.txt"
    val file = new File(path)
    if (file.exists()) {
      file.delete()
    }
    assert(!new File(path).exists())
    IOUtils.createFile(path)
    assert(new File(path).exists())

  }


}
