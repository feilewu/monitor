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

import com.github.feilewu.monitor.network.TransportChannelHandler;
import com.github.feilewu.monitor.network.TransportContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author: pf_xu
 * @Date: 2024/4/16 0:52
 * @email：pfxuchn@gmail.com
 */
public class TransportClientFactory implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(TransportClientFactory.class);

    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private final TransportContext context;

    private int connCreateTimeout = 10000;

    public TransportClientFactory(TransportContext context) {
        this.context = context;
    }

    /** Create a completely new {@link TransportClient} to the remote address. */
    public TransportClient createClient(InetSocketAddress address)
            throws IOException, InterruptedException {
        logger.debug("Creating new connection to {}", address);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                // Disable Nagle's Algorithm since we don't want packets to wait
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connCreateTimeout);

        final AtomicReference<TransportClient> clientRef = new AtomicReference<>();
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                TransportChannelHandler clientHandler = context.initializePipeline(ch);
                clientRef.set(clientHandler.getClient());
            }
        });
        // Connect to the remote server
        ChannelFuture cf = bootstrap.connect(address);
        if (connCreateTimeout <= 0) {
            cf.awaitUninterruptibly();
            assert cf.isDone();
            if (cf.isCancelled()) {
                throw new IOException(String.format("Connecting to %s cancelled", address));
            } else if (!cf.isSuccess()) {
                throw new IOException(String.format("Failed to connect to %s", address),
                        cf.cause());
            }
        } else if (!cf.await(connCreateTimeout)) {
            throw new IOException(
                    String.format("Connecting to %s timed out (%s ms)",
                            address, connCreateTimeout));
        } else if (cf.cause() != null) {
            throw new IOException(String.format("Failed to connect to %s", address), cf.cause());
        }
        TransportClient client = clientRef.get();
        assert client != null : "Channel future completed successfully with null client";
        logger.info("Successfully created connection to {}", address);
        return client;
    }

    @Override
    public void close() {
        workerGroup.shutdownGracefully();
    }
}
