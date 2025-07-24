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

import org.apache.pekko.actor.SupervisorStrategy;
import org.apache.pekko.japi.Function;

import java.sql.SQLException;


public class SupervisorFunction implements Function<Throwable, SupervisorStrategy.Directive> {

    @Override
    public SupervisorStrategy.Directive apply(Throwable param) {
        switch (param) {
            case ArithmeticException arithmeticException -> {
                System.out.println("meet ArithmeticException,just resume");
                return SupervisorStrategy.resume();
            }
            case NullPointerException nullPointerException -> {
                System.out.println("meet NullPointerException,restart");
                return SupervisorStrategy.restart();
            }
            case IllegalArgumentException illegalArgumentException -> {
                return SupervisorStrategy.stop();
            }
            case SQLException throwables -> {
                return SupervisorStrategy.restart();
            }
            case null, default -> {
                return SupervisorStrategy.escalate();
            }
        }
    }
}
