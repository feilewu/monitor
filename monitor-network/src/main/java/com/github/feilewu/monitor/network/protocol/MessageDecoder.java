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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

/**
 * @Author: pf_xu
 * @Date: 2024/4/20 10:20
 * @email：pfxuchn@gmail.com
 */
public class MessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        Message.Type type = Message.Type.decode(buf);
        Message decoded = decode(type, buf);
        out.add(decoded);
    }

    private Message decode(Message.Type type, ByteBuf buf) {
        switch (type) {
            case RpcRequest:
                return RpcRequest.decode(buf);
            case RpcResponse:
                return RpcResponse.decode(buf);
            case OneWayMessage:
                return OneWayMessage.decode(buf);
            default: throw new IllegalArgumentException("Unknown type: " + type.id());
        }
    }
}
