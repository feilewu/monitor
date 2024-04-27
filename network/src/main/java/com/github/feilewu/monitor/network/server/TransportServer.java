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

import com.github.feilewu.monitor.network.TransportContext;
import com.github.feilewu.monitor.network.NettyUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @Author: pf_xu
 * @Date: 2024/4/15 22:23
 * @email：pfxuchn@gmail.com
 */
public class TransportServer implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(TransportServer.class);

    private final TransportContext context;

    private ServerBootstrap bootstrap;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private ChannelFuture channelFuture;

    private final RpcHandler rpcHandler;

    private int port;

    public TransportServer(TransportContext context, RpcHandler rpcHandler) {
        this.context = context;
        this.rpcHandler = rpcHandler;
    }

    public void bind(String hostToBind, int portToBind) {

        bossGroup = NettyUtils.createEventLoop(1,
                "monitor" + "-boss");
        workerGroup =  NettyUtils.createEventLoop(4,
                "monitor" + "-server");

        bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class);

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                logger.debug("New connection accepted for remote address {}.", ch.remoteAddress());
                context.initializePipeline(ch, rpcHandler);
            }
        });
        InetSocketAddress address = hostToBind == null ?
                new InetSocketAddress(portToBind): new InetSocketAddress(hostToBind, portToBind);
        channelFuture = bootstrap.bind(address);
        channelFuture.syncUninterruptibly();
        InetSocketAddress localAddress = (InetSocketAddress) channelFuture.channel().localAddress();
        logger.info("Transport server started on {} with port {}",
                localAddress.getHostString(), localAddress.getPort());
        port = localAddress.getPort();
    }

    public int getPort() {
        return port;
    }

    @Override
    public void close() throws IOException {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }
}
