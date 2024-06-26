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
 * @Date: 2024/4/28 21:12
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.conf.config

private[monitor] object Config {

  private[monitor] val MONITOR_MASTER = ConfigBuilder("monitor.master")
    .stringConf()
    .createWithDefault("monitor://127.0.0.1:7077")

  private[monitor] val EXECUTOR_LOG_DIR = ConfigBuilder("monitor.executor.log.dir")
    .stringConf()
    .createWithDefault("/tmp/monitor/executor")


  private[monitor] val MASTER_UI_ENABLED = ConfigBuilder("monitor.master.ui.enabled")
    .booleanConf()
    .createWithDefault(true)

  private[monitor] val MASTER_UI_SERVER_CLASS = ConfigBuilder("monitor.master.ui.server.class")
    .stringConf()
    .createWithDefault("com.github.feilewu.monitor.ui.JettyServer")

}
