package com.github.feilewu.monitor.network.server;
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

import com.github.feilewu.monitor.network.Throws;
import com.github.feilewu.monitor.network.buffer.NioManagedBuffer;
import com.github.feilewu.monitor.network.client.RpcResponseCallback;
import com.github.feilewu.monitor.network.client.TransportClient;
import com.github.feilewu.monitor.network.protocol.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * @Author: pf_xu
 * @Date: 2024/4/16 0:19
 * @email：pfxuchn@gmail.com
 */
public class TransportRequestHandler extends MessageHandler<RequestMessage> {

    private static final Logger logger = LoggerFactory.getLogger(TransportRequestHandler.class);

    private final RpcHandler rpcHandler;

    /** The Netty channel that this handler is associated with. */
    private final Channel channel;

    private final TransportClient reverseClient;

    public TransportRequestHandler(RpcHandler rpcHandler, Channel channel, TransportClient reverseClient) {
        this.rpcHandler = rpcHandler;
        this.channel = channel;
        this.reverseClient = reverseClient;
    }

    @Override
    public void handle(RequestMessage message) throws Exception {
        if (message instanceof RpcRequest) {
            rpcHandler.receive(reverseClient, message.body().nioByteBuffer(),
                new RpcResponseCallback() {
                    @Override
                    public void onSuccess(ByteBuffer response) {
                        respond(new RpcResponse(((RpcRequest) message).requestId,
                                new NioManagedBuffer(response)));
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        respond(new RpcFailure(((RpcRequest) message).requestId,
                                Throws.getStackTraceAsString(e)));
                    }
            });


        }


    }

    @Override
    public void channelActive() {

    }

    @Override
    public void exceptionCaught(Throwable cause) {

    }

    @Override
    public void channelInactive() {

    }

    /**
     * Responds to a single message with some Encodable object. If a failure occurs while sending,
     * it will be logged and the channel closed.
     */
    private ChannelFuture respond(Encodable result) {
        SocketAddress remoteAddress = channel.remoteAddress();
        return channel.writeAndFlush(result).addListener(future -> {
            if (future.isSuccess()) {
                logger.trace("Sent result {} to client {}", result, remoteAddress);
            } else {
                logger.error(String.format("Error sending result %s to %s; closing connection",
                        result, remoteAddress), future.cause());
                channel.close();
            }
        });
    }
}
