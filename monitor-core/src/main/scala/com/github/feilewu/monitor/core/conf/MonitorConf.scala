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
 * @Date: 2024/4/27 8:29
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.conf

import java.util.concurrent.ConcurrentHashMap

import com.github.feilewu.monitor.core.conf.config.ConfigEntry

private[monitor] class MonitorConf {

  private val map = new ConcurrentHashMap[String, String]()

  def set(key: String, value: String): MonitorConf = {
    if (key == null) {
      throw new NullPointerException("null key")
    }
    if (value == null) {
      throw new NullPointerException("null value for " + key)
    }
    map.put(key, value)
    this
  }


  def get[T](configEntry: ConfigEntry[T]): T = {
    getOption(configEntry).get
  }

  def getOption[T](configEntry: ConfigEntry[T]): Option[T] = {
    val value = map.get(configEntry.key)
    if (value == null) {
      configEntry.defaultValue
    } else {
      Some(configEntry.valueConverter(value))
    }
  }


}
