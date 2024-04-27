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

import com.github.feilewu.monitor.network.buffer.ManagedBuffer;

/**
 * @Author: pf_xu
 * @Date: 2024/4/16 23:20
 * @email：pfxuchn@gmail.com
 */
public abstract class AbstractMessage implements Message {

    private final ManagedBuffer body;

    protected AbstractMessage() {
        this(null);
    }

    protected AbstractMessage(ManagedBuffer body) {
        this.body = body;
    }

    @Override
    public ManagedBuffer body() {
        return body;
    }
}
