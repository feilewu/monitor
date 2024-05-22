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

import com.github.feilewu.monitor.network.client.TransportClient;
import com.github.feilewu.monitor.network.client.TransportClientFactory;
import com.github.feilewu.monitor.network.client.TransportResponseHandler;
import com.github.feilewu.monitor.network.protocol.FrameDecoder;
import com.github.feilewu.monitor.network.protocol.MessageDecoder;
import com.github.feilewu.monitor.network.protocol.MessageEncoder;
import com.github.feilewu.monitor.network.server.RpcHandler;
import com.github.feilewu.monitor.network.server.TransportRequestHandler;
import com.github.feilewu.monitor.network.server.TransportServer;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @Author: pf_xu
 * @Date: 2024/4/16 0:02
 * @email：pfxuchn@gmail.com
 */
public class TransportContext {
    private static final Logger logger = LoggerFactory.getLogger(TransportContext.class);

    private final RpcHandler rpcHandler;

    private TransportClientFactory clientFactory = null;

    public TransportContext(RpcHandler rpcHandler) {
        if (rpcHandler == null) {
            throw new IllegalArgumentException("rpcHandler can not be null");
        }
        this.rpcHandler = rpcHandler;
        clientFactory = new TransportClientFactory(this);
    }

    public TransportServer createTransportServer() {
        return new TransportServer(this, rpcHandler);
    }

    public TransportClient createClient(InetSocketAddress address)
            throws IOException, InterruptedException {
        return clientFactory.createClient(address);
    }

    public TransportClient createClient(String host, int port)
            throws IOException, InterruptedException {
        return createClient(new InetSocketAddress(host, port));
    }

    public TransportChannelHandler initializePipeline(
            SocketChannel channel,
            RpcHandler rpcHandler) {
        TransportChannelHandler transportChannelHandler = createChannelHandler(channel, rpcHandler);
        channel.pipeline()
                .addLast("frameDecoder", new FrameDecoder())
                .addLast("messageDecoder", new MessageDecoder())
                .addLast("messageEncoder", new MessageEncoder())
                .addLast(TransportChannelHandler.NAME, transportChannelHandler);
        return transportChannelHandler;
    }

    public TransportChannelHandler initializePipeline(
            SocketChannel channel) {
        return initializePipeline(channel, null);
    }

    private TransportChannelHandler createChannelHandler(Channel channel, RpcHandler rpcHandler) {
        TransportResponseHandler responseHandler = new TransportResponseHandler(channel);
        TransportClient client = new TransportClient(channel, responseHandler);
        TransportRequestHandler requestHandler = new TransportRequestHandler(rpcHandler, channel, client);
        return new TransportChannelHandler(client, requestHandler, responseHandler);
    }

    public void close() {
        clientFactory.close();
    }

}
