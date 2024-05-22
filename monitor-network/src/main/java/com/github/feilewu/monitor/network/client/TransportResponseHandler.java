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

import com.github.feilewu.monitor.network.protocol.MessageHandler;
import com.github.feilewu.monitor.network.protocol.ResponseMessage;
import com.github.feilewu.monitor.network.protocol.RpcFailure;
import com.github.feilewu.monitor.network.protocol.RpcResponse;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.feilewu.monitor.network.NettyUtils.getRemoteAddress;

/**
 * @Author: pf_xu
 * @Date: 2024/4/16 0:19
 * @email：pfxuchn@gmail.com
 */
public class TransportResponseHandler extends MessageHandler<ResponseMessage> {

    private static final Logger logger = LoggerFactory.getLogger(TransportResponseHandler.class);

    private final Channel channel;

    private final Map<Long, BaseResponseCallback> outstandingRpcs;

    public TransportResponseHandler(Channel channel) {
        this.channel = channel;
        this.outstandingRpcs = new ConcurrentHashMap<>();
    }

    @Override
    public void handle(ResponseMessage message) throws Exception {
        if (message instanceof RpcResponse) {
            RpcResponse rpcResponse = (RpcResponse)message;
            long requestId = rpcResponse.requestId;
            RpcResponseCallback callback = (RpcResponseCallback)outstandingRpcs.get(requestId);
            if (callback == null) {
                logger.warn("Ignoring response for RPC {} from {} ({} bytes) since it is not outstanding",
                        rpcResponse.requestId, getRemoteAddress(channel), rpcResponse.body().size());
            } else {
                callback.onSuccess(rpcResponse.body().nioByteBuffer());
            }
        } else if (message instanceof RpcFailure) {
            RpcFailure resp = (RpcFailure) message;
            BaseResponseCallback listener = outstandingRpcs.get(resp.requestId);
            if (listener == null) {
                logger.warn("Ignoring response for RPC {} from {} ({}) since it is not outstanding",
                        resp.requestId, getRemoteAddress(channel), resp.errorString);
            } else {
                outstandingRpcs.remove(resp.requestId);
                listener.onFailure(new RuntimeException(resp.errorString));
            }

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

    public void addRpcRequest(long requestId, BaseResponseCallback callback) {
        outstandingRpcs.put(requestId, callback);
    }

    public void removeRpcRequest(long requestId) {
        outstandingRpcs.remove(requestId);
    }

}
