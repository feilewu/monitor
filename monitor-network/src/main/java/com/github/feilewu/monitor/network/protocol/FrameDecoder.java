package com.github.feilewu.monitor.network.protocol;
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

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: pf_xu
 * @Date: 2024/4/17 20:18
 * @email：pfxuchn@gmail.com
 */

/**
 *   lengthFieldOffset   = 0
 *   lengthFieldLength   = 8
 *   lengthAdjustment    = 0
 *   initialBytesToStrip = 8
 * <p>
 *   one frame
 *   +----------+----------+------+
 *   |  Length    | Frame Content |
 *   |  0x00000C  | bytes         |
 *   +----------+----------+------+
 *   message
 *   +----------+----------+------+
 *   |  Header           | Body |
 *   |  type + bodyMeta  |  body   |
 *   +----------+----------+------+
 */

public class FrameDecoder extends LengthFieldBasedFrameDecoder {

    private static final Logger logger = LoggerFactory.getLogger(FrameDecoder.class);

    /**
     *
     * @param maxFrameLength  帧的最大长度
     * @param lengthFieldOffset length字段偏移的地址
     * @param lengthFieldLength length字段所占的字节长
     * @param lengthAdjustment 修改帧数据长度字段中定义的值，可以为负数 因为有时候我们习惯把头部记入长度,若为负数,则说明要推后多少个字段
     * @param initialBytesToStrip 解析时候跳过多少个长度
     */
    public FrameDecoder() {
        super(1024*1024, 0, 8, 0, 8);
    }
}
