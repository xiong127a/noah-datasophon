/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.datasophon.worker.actor;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import com.datasophon.common.command.PingCommand;
import com.datasophon.common.utils.ExecResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 发送 ping，返回 pong
 *
 * @author zhenqin
 */
public class PingActor extends AbstractActor {
    
    private static final Logger logger = LoggerFactory.getLogger(PingActor.class);

    @Override
    public void preStart() {
        logger.info("PingActor已启动，路径: {}", getSelf().path());
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(PingCommand.class, command -> {
                    logger.info("收到Ping命令，来自: {}, 消息: {}", getSender().path(), command.getMessage());
                    ExecResult execResult = new ExecResult();
                    execResult.setExecResult(true);
                    execResult.setExecOut("pong");
                    getSender().tell(execResult, getSelf());
                    logger.info("已回复Pong到: {}", getSender().path());
                })
                .matchAny(msg -> {
                    logger.warn("收到未知消息类型: {}, 来自: {}", msg.getClass().getName(), getSender().path());
                    unhandled(msg);
                })
                .build();
    }
}
