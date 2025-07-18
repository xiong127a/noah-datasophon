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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;

/**
 * Actor for handling remote events in Akka Artery remoting.
 * Note: Classic remoting events (AssociationErrorEvent, AssociatedEvent,
 * DisassociatedEvent)
 * are no longer available in Akka 2.10.7-M1 with Artery remoting.
 */
public class RemoteEventActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(RemoteEventActor.class);

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .matchAny(msg -> {
                    logger.debug("Received remote event: {}", msg);
                    unhandled(msg);
                })
                .build();
    }
}
