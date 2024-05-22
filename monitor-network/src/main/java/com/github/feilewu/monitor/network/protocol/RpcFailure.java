package com.github.feilewu.monitor.network.protocol;
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

import io.netty.buffer.ByteBuf;

/**
 * @Author: pf_xu
 * @Date: 2024/4/16 23:49
 * @email：pfxuchn@gmail.com
 */
public class RpcFailure extends ResponseMessage {

    public final long requestId;
    public final String errorString;

    public RpcFailure(long requestId, String errorString) {
        this.requestId = requestId;
        this.errorString = errorString;
    }

    @Override
    public Message.Type type() { return Type.RpcFailure; }

    @Override
    public int encodedLength() {
        return 8 + Encoders.Strings.encodedLength(errorString);
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeLong(requestId);
        Encoders.Strings.encode(buf, errorString);
    }

}
