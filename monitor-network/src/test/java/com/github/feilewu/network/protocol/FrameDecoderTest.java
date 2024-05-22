package com.github.feilewu.network.protocol;

import com.github.feilewu.monitor.network.buffer.NioManagedBuffer;
import com.github.feilewu.monitor.network.protocol.FrameDecoder;
import com.github.feilewu.monitor.network.protocol.Message;
import com.github.feilewu.monitor.network.protocol.RpcRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

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
 * @Date: 2024/4/18 22:50
 * @email：pfxuchn@gmail.com
 */
class FrameDecoderTest {

    @Test
    void decodeTest() throws IOException {
        ByteBuf buf = Unpooled.buffer();
        String message = "hello world";
        RpcRequest rpcRequest = new RpcRequest(1234567,
                new NioManagedBuffer(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8))));
        int headerLength = rpcRequest.type().encodedLength() + rpcRequest.encodedLength();
        long frameLength = headerLength + rpcRequest.body().size();
        buf.writeLong(frameLength);
        rpcRequest.type().encode(buf);
        rpcRequest.encode(buf);
        buf.writeBytes(rpcRequest.body().nioByteBuffer());
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new FrameDecoder());
        boolean writeInbound = embeddedChannel.writeInbound(buf);
        assertTrue(writeInbound);

        ByteBuf read = embeddedChannel.readInbound();
        assertEquals(rpcRequest.type(), Message.Type.decode(read));
        RpcRequest request = RpcRequest.decode(read);
        assertEquals(rpcRequest.requestId, request.requestId);
        String receivedMessage = StandardCharsets.UTF_8.decode(request.body().nioByteBuffer()).toString();
        assertEquals(message, receivedMessage);
    }

}
