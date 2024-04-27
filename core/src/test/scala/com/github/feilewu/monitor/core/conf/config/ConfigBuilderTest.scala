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
 * @Date: 2024/4/27 11:37
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.conf.config

import java.util.concurrent.TimeUnit

import org.scalatest.funsuite.AnyFunSuite


class ConfigBuilderTest extends AnyFunSuite {

  test("test intConf") {

    val MONITOR_TEST_COUNT = ConfigBuilder("monitor.test.count")
      .intConf()
      .createWithDefault(10)

    assert("monitor.test.count".equals(MONITOR_TEST_COUNT.key))
    assert(MONITOR_TEST_COUNT.defaultValue.get == 10)

  }

  test("test stringConf") {

    val MONITOR_TEST_NAME = ConfigBuilder("monitor.test.name")
      .stringConf()
      .createWithDefault("MONITOR")

    assert(MONITOR_TEST_NAME.defaultValue.get.equals("MONITOR"))

  }

  test("test timeConf") {

    val MONITOR_TEST_TIMEOUT = ConfigBuilder("monitor.test.timeout")
      .timeConf(TimeUnit.SECONDS)
      .createWithDefaultString("1d")

    assert(MONITOR_TEST_TIMEOUT.defaultValue.get == 1 * 24 * 3600)

  }

}
