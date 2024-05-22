package com.github.feilewu.network;

import com.github.feilewu.monitor.network.TransportContext;
import com.github.feilewu.monitor.network.client.RpcResponseCallback;
import com.github.feilewu.monitor.network.client.TransportClient;
import com.github.feilewu.monitor.network.server.RpcHandler;
import com.github.feilewu.monitor.network.server.TransportServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

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

/**
 * @Author: pf_xu
 * @Date: 2024/4/20 14:56
 * @email：pfxuchn@gmail.com
 */
class TransportContextTest {

    @Test
    void testTransport() throws IOException, InterruptedException {
        TransportContext context = new TransportContext(new RpcHandler() {
            @Override
            protected void receive(TransportClient client, ByteBuffer message, RpcResponseCallback callback) {

            }
        });
        TransportServer transportServer = context.createTransportServer();
        transportServer.bind("localhost", 10010);
        TransportClient client = context.createClient(new InetSocketAddress("localhost", 10010));
        transportServer.close();
        context.close();
    }

}
