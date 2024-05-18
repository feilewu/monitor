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
 * @Date: 2024/4/28 21:06
 * @email：pfxuchn@gmail.com
 */
package com.github.feilewu.monitor.core.deploy

import com.github.feilewu.monitor.core.rpc.{RpcAddress, RpcEndpointRef}
case class RegisterAgent(cores: String, memory: String,
                         agentRef: RpcEndpointRef) extends DeployMessage


class CommandMessage extends DeployMessage


case class ExecuteV2ry() extends CommandMessage


class DeployMessage extends Serializable

case class HeartBeat(rpcAddress: RpcAddress) extends DeployMessage
