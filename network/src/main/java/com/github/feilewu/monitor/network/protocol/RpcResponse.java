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

import com.github.feilewu.monitor.network.buffer.NioManagedBuffer;
import com.github.feilewu.monitor.network.buffer.ManagedBuffer;
import io.netty.buffer.ByteBuf;

/**
 * @Author: pf_xu
 * @Date: 2024/4/16 23:38
 * @email：pfxuchn@gmail.com
 */
public class RpcResponse extends ResponseMessage {

    public final long requestId;

    public RpcResponse(long requestId, ManagedBuffer message) {
        super(message);
        this.requestId = requestId;
    }

    @Override
    public Type type() {
        return Type.RpcResponse;
    }

    @Override
    public int encodedLength() {
        return 8 + 4;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeLong(requestId);
        // See comment in encodedLength().
        buf.writeInt((int) body().size());
    }

    public static RpcResponse decode(ByteBuf buf) {
        long requestId = buf.readLong();
        buf.readInt();
        return new RpcResponse(requestId, new NioManagedBuffer(buf.nioBuffer()));
    }
}
