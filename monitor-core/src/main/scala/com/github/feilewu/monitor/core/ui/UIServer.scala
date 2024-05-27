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
 * @Date: 2024/5/26 10:16
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.ui

import com.github.feilewu.monitor.core.conf.MonitorConf
import com.github.feilewu.monitor.core.deploy.master.Master


private[monitor] class UIServerException(message: String, cause: Throwable)
  extends RuntimeException {

  def this(throwable: Throwable) = {
    this(null, throwable)
  }

}


private[monitor] trait UIServer {

  def init(conf: MonitorConf, master: Master)

  def start()

  def stop()

}
