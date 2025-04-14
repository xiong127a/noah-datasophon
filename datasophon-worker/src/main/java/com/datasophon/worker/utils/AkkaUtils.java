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

package com.datasophon.worker.utils;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.datasophon.common.model.TemplateRequestMessage;
import com.datasophon.common.model.TemplateResponseMessage;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Akka工具类，用于Actor间通信
 */
@Slf4j
public class AkkaUtils {

    /**
     * 请求超时时间（秒）
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    /**
     * 从Master获取模板内容
     * 
     * @param actorSystem  Akka系统实例
     * @param masterHost   Master主机地址
     * @param templateName 模板名称
     * @return 模板内容，如果获取失败则返回null
     */
    public static String getTemplateContent(ActorSystem actorSystem, String masterHost, String templateName) {
        return getTemplateContent(actorSystem, masterHost, templateName, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 从Master获取模板内容（带超时参数）
     * 
     * @param actorSystem    Akka系统实例
     * @param masterHost     Master主机地址
     * @param templateName   模板名称
     * @param timeoutSeconds 超时时间（秒）
     * @return 模板内容，如果获取失败则返回null
     */
    public static String getTemplateContent(ActorSystem actorSystem, String masterHost, String templateName,
            int timeoutSeconds) {
        try {
            // 创建请求消息
            TemplateRequestMessage request = new TemplateRequestMessage();
            request.setRequestType(TemplateRequestMessage.RequestType.CONTENT);
            request.setTemplateName(templateName);

            // 获取模板服务Actor
            ActorSelection actorSelection = actorSystem.actorSelection(
                    "akka.tcp://datasophon@" + masterHost + ":2551/user/templateServiceActor");

            // 发送请求并等待响应
            Timeout timeout = new Timeout(Duration.create(timeoutSeconds, TimeUnit.SECONDS));
            Future<Object> future = Patterns.ask(actorSelection, request, timeout);
            TemplateResponseMessage response = (TemplateResponseMessage) Await.result(future,
                    Duration.create(timeoutSeconds, TimeUnit.SECONDS));

            // 处理响应
            if (response.isSuccess() && response.getResponseType() == TemplateResponseMessage.ResponseType.CONTENT) {
                log.info("成功从Master获取模板: {}, 内容长度: {}", templateName,
                        response.getTemplateContent() == null ? 0 : response.getTemplateContent().length());
                return response.getTemplateContent();
            } else {
                log.error("从Master获取模板失败: {}, 错误: {}", templateName, response.getErrorMessage());
                return null;
            }
        } catch (Exception e) {
            log.error("请求模板时发生异常: {}", templateName, e);
            return null;
        }
    }

    /**
     * 从Master获取模板列表
     * 
     * @param actorSystem Akka系统实例
     * @param masterHost  Master主机地址
     * @return 模板列表，如果获取失败则返回null
     */
    public static List<String> getTemplateList(ActorSystem actorSystem, String masterHost) {
        return getTemplateList(actorSystem, masterHost, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 从Master获取模板列表（带超时参数）
     * 
     * @param actorSystem    Akka系统实例
     * @param masterHost     Master主机地址
     * @param timeoutSeconds 超时时间（秒）
     * @return 模板列表，如果获取失败则返回null
     */
    public static List<String> getTemplateList(ActorSystem actorSystem, String masterHost, int timeoutSeconds) {
        try {
            // 创建请求消息
            TemplateRequestMessage request = new TemplateRequestMessage();
            request.setRequestType(TemplateRequestMessage.RequestType.LIST);

            // 获取模板服务Actor
            ActorSelection actorSelection = actorSystem.actorSelection(
                    "akka.tcp://datasophon@" + masterHost + ":2551/user/templateServiceActor");

            // 发送请求并等待响应
            Timeout timeout = new Timeout(Duration.create(timeoutSeconds, TimeUnit.SECONDS));
            Future<Object> future = Patterns.ask(actorSelection, request, timeout);
            TemplateResponseMessage response = (TemplateResponseMessage) Await.result(future,
                    Duration.create(timeoutSeconds, TimeUnit.SECONDS));

            // 处理响应
            if (response.isSuccess() && response.getResponseType() == TemplateResponseMessage.ResponseType.LIST) {
                log.info("成功从Master获取模板列表, 共 {} 个模板", response.getTemplateList().size());
                return response.getTemplateList();
            } else {
                log.error("从Master获取模板列表失败: {}", response.getErrorMessage());
                return null;
            }
        } catch (Exception e) {
            log.error("请求模板列表时发生异常", e);
            return null;
        }
    }
}