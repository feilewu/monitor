package com.github.feilewu.monitor.network.protocol;/*
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

import com.github.feilewu.monitor.network.buffer.ManagedBuffer;
import com.github.feilewu.monitor.network.buffer.NioManagedBuffer;
import io.netty.buffer.ByteBuf;

/**
 * @Author: pf_xu
 * @Date: 2024/4/27 21:00
 * @email：pfxuchn@gmail.com
 */
public class OneWayMessage extends RequestMessage{
    public OneWayMessage(ManagedBuffer body) {
        super(body);
    }

    @Override
    public int encodedLength() {
        return 4;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeInt((int) body().size());
    }

    @Override
    public Type type() {
        return Type.OneWayMessage;
    }

    public static OneWayMessage decode(ByteBuf buf) {
        // See comment in encodedLength().
        buf.readInt();
        return new OneWayMessage(new NioManagedBuffer(buf.nioBuffer()));
    }
}
