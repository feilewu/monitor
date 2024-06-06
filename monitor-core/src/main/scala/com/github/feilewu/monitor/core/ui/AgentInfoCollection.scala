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
 * @Date: 2024/6/2 19:09
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.ui

class AgentInfoCollection(agents: List[AgentInfo]) extends Iterable[AgentInfo] {

  override def iterator: Iterator[AgentInfo] = new Iterator[AgentInfo] {
    private var head: Int = -1
    private val agentInfoSize = agents.length

    override def hasNext: Boolean = {
      if (agents == null || agents.isEmpty) {
        return false
      }
      head + 1 < agentInfoSize
    }

    override def next(): AgentInfo = {
      head = head + 1
      agents(head)
    }
  }
}
