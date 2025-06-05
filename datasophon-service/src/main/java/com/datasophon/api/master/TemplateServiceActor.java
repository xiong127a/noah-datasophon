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

package com.datasophon.api.master;

import akka.actor.UntypedActor;
import com.datasophon.api.utils.TemplatePathUtils;
import com.datasophon.common.model.TemplateRequestMessage;
import com.datasophon.common.model.TemplateResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

/**
 * 处理Worker请求模板的Actor
 */

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class TemplateServiceActor extends UntypedActor {


    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof TemplateRequestMessage) {
            TemplateRequestMessage request = (TemplateRequestMessage) message;
            TemplateResponseMessage response = new TemplateResponseMessage();

            try {

                // 处理获取模板内容请求
                handleContentRequest(request, response);

            } catch (Exception e) {
                log.error("处理模板请求时发生错误", e);
                response.setSuccess(false);
                response.setErrorMessage("处理请求时发生错误: " + e.getMessage());
            }

            // 回复请求
            getSender().tell(response, getSelf());
        } else {
            unhandled(message);
        }
    }

    /**
     * 处理获取模板内容的请求
     */
    private void handleContentRequest(TemplateRequestMessage request, TemplateResponseMessage response) {
        log.info("收到模板内容请求: {}", request.getTemplateName());
        String templateName = request.getTemplateName();

        if (templateName == null || templateName.trim().isEmpty()) {
            response.setSuccess(false);
            response.setErrorMessage("模板名称不能为空");
            return;
        }

        String content = TemplatePathUtils.getTemplateContent(templateName);
        if (content == null) {
            response.setSuccess(false);
            response.setErrorMessage("模板不存在: " + templateName);
            return;
        }

        response.setSuccess(true);
        response.setTemplateContent(content);
        log.info("成功获取模板: {}, 内容长度: {}", templateName, content.length());
    }
}