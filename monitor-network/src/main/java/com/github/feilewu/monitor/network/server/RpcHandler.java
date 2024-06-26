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

import com.github.feilewu.monitor.network.client.RpcResponseCallback;
import com.github.feilewu.monitor.network.client.TransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * @Author: pf_xu
 * @Date: 2024/4/16 0:09
 * @email：pfxuchn@gmail.com
 */
public abstract class RpcHandler {
    private final OneWayRpcCallback ONE_WAY_RPC_CALLBACK = new OneWayRpcCallback();
    /**
     * Receive a single RPC message. Any exception thrown while in this method will be sent back to
     * the client in string form as a standard RPC failure.
     *
     * Neither this method nor #receiveStream will be called in parallel for a single
     * TransportClient (i.e., channel).
     *
     * @param client A channel client which enables the handler to make requests back to the sender
     *               of this RPC. This will always be the exact same object for a particular channel.
     * @param message The serialized bytes of the RPC.
     * @param callback Callback which should be invoked exactly once upon success or failure of the
     *                 RPC.
     */
    protected abstract void receive(
            TransportClient client,
            ByteBuffer message,
            RpcResponseCallback callback);


    protected void receive(TransportClient client, ByteBuffer message) {
        receive(client, message, ONE_WAY_RPC_CALLBACK);
    }

    public static final class OneWayRpcCallback implements RpcResponseCallback {

        private static final Logger logger = LoggerFactory.getLogger(OneWayRpcCallback.class);

        @Override
        public void onSuccess(ByteBuffer response) {
            logger.warn("Response provided for one-way RPC.");
        }

        @Override
        public void onFailure(Throwable e) {
            logger.error("Error response provided for one-way RPC.", e);
        }

    }

}


