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

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.ThreadFactory;

/**
 * @Author: pf_xu
 * @Date: 2024/4/15 22:32
 * @email：pfxuchn@gmail.com
 */
public class NettyUtils {

    public static EventLoopGroup createEventLoop(int numThreads, String threadPrefix) {
        ThreadFactory threadFactory = createThreadFactory(threadPrefix);
        return new NioEventLoopGroup(numThreads, threadFactory);
    }

    public static ThreadFactory createThreadFactory(String threadPoolPrefix) {
        return new DefaultThreadFactory(threadPoolPrefix, true);
    }

    public static String getRemoteAddress(Channel channel) {
        if (channel != null && channel.remoteAddress() != null) {
            return channel.remoteAddress().toString();
        }
        return "<unknown remote>";
    }

}
