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
 * @Date: 2024/4/29 21:03
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.util

import org.scalatest.funsuite.AnyFunSuite

import com.github.feilewu.monitor.core.rpc.RpcAddress



class UtilsTest extends AnyFunSuite{

  test("parseMasterUrls") {
    val masterUrls1 = "monitor://10.10.10.10:1000,10.10.10.9:1001"
    val defined = Utils.parseMasterUrls(masterUrls1)
      .contains(RpcAddress("10.10.10.9", 1001))
    assert(defined)

  }


}
