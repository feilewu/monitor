package com.github.feilewu.monitor.network;
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

import com.github.feilewu.monitor.network.protocol.Message;
import com.github.feilewu.monitor.network.client.TransportClient;
import com.github.feilewu.monitor.network.client.TransportResponseHandler;
import com.github.feilewu.monitor.network.protocol.RequestMessage;
import com.github.feilewu.monitor.network.protocol.ResponseMessage;
import com.github.feilewu.monitor.network.server.TransportRequestHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @Author: pf_xu
 * @Date: 2024/4/16 0:22
 * @email：pfxuchn@gmail.com
 */
public class TransportChannelHandler extends SimpleChannelInboundHandler<Message> {

    public static final String NAME = "transportChannelHandler";

    private final TransportClient client;

    private final TransportRequestHandler requestHandler;

    private final TransportResponseHandler responseHandler;

    public TransportChannelHandler(TransportClient client,
                                   TransportRequestHandler requestHandler,
                                   TransportResponseHandler responseHandler) {
        this.client = client;
        this.requestHandler = requestHandler;
        this.responseHandler = responseHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Message message)
            throws Exception {
        if (message instanceof RequestMessage) {
            requestHandler.handle((RequestMessage) message);
        } else if (message instanceof ResponseMessage) {
            responseHandler.handle((ResponseMessage) message);
        }
    }

    public TransportClient getClient() {
        return client;
    }
}
