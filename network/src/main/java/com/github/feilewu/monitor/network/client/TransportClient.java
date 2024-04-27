package com.github.feilewu.monitor.network.client;
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
import com.github.feilewu.monitor.network.protocol.RpcRequest;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import static com.github.feilewu.monitor.network.NettyUtils.getRemoteAddress;

/**
 * @Author: pf_xu
 * @Date: 2024/4/16 0:15
 * @email：pfxuchn@gmail.com
 */
public class TransportClient {

    private static final Logger logger = LoggerFactory.getLogger(TransportClient.class);

    private final Channel channel;
    private  final TransportResponseHandler handler;
    private String clientId;

    public TransportClient(Channel channel, TransportResponseHandler handler) {
        this.channel = channel;
        this.handler = handler;
    }

    public Channel getChannel() {
        return channel;
    }

    public long sendRpc(ByteBuffer buffer, RpcResponseCallback callback) {
        long requestId = requestId();
        handler.addRpcRequest(requestId, callback);
        RpcRequest rpcRequest = new RpcRequest(requestId, new NioManagedBuffer(buffer));
        RpcChannelListener rpcChannelListener = new RpcChannelListener(requestId, callback);
        channel.writeAndFlush(rpcRequest).addListener(rpcChannelListener);
        return requestId;
    }

    private static long requestId() {
        return Math.abs(UUID.randomUUID().getLeastSignificantBits());
    }

    private class StdChannelListener
            implements GenericFutureListener<Future<? super Void>> {
        final long startTime;
        final Object requestId;

        StdChannelListener(Object requestId) {
            this.startTime = System.currentTimeMillis();
            this.requestId = requestId;
        }

        @Override
        public void operationComplete(Future<? super Void> future) throws Exception {
            if (future.isSuccess()) {
                if (logger.isTraceEnabled()) {
                    long timeTaken = System.currentTimeMillis() - startTime;
                    logger.trace("Sending request {} to {} took {} ms", requestId,
                            getRemoteAddress(channel), timeTaken);
                }
            } else {
                String errorMsg = String.format("Failed to send RPC %s to %s: %s", requestId,
                        getRemoteAddress(channel), future.cause());
                logger.error(errorMsg, future.cause());
                channel.close();
                try {
                    handleFailure(errorMsg, future.cause());
                } catch (Exception e) {
                    logger.error("Uncaught exception in RPC response callback handler!", e);
                }
            }
        }

        void handleFailure(String errorMsg, Throwable cause) throws Exception {}
    }

    private class RpcChannelListener extends StdChannelListener {
        final long rpcRequestId;
        final BaseResponseCallback callback;

        RpcChannelListener(long rpcRequestId, BaseResponseCallback callback) {
            super("RPC " + rpcRequestId);
            this.rpcRequestId = rpcRequestId;
            this.callback = callback;
        }

        @Override
        void handleFailure(String errorMsg, Throwable cause) {
            handler.removeRpcRequest(rpcRequestId);
            callback.onFailure(new IOException(errorMsg, cause));
        }
    }


}
