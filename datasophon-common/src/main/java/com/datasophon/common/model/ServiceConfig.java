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

package com.datasophon.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 配置参数详情
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceConfig implements Serializable {

    private String name;

    private Object value;

    private String label;

    private String description;

    private boolean required;

    private String type;

    private boolean configurableInWizard;

    private Object defaultValue;

    private Integer minValue;

    private Integer maxValue;

    private String unit;

    private boolean hidden;

    private List<String> selectValue;

    private String configType;

    private boolean configWithKerberos;

    private boolean configWithRack;

    private boolean configWithHA;
    /**
     * 用于设置分隔符，即生成类似 [1, 2, 3] 这样的格式。
     */
    private String separator;
    /**
     * 用于设置开始和结束符号，即生成类似 [1, 2, 3] 这样的格式。
     */
    private String open;
    /**
     * 用于设置开始和结束符号，即生成类似 [1, 2, 3] 这样的格式。
     */
    private String close;

    private String configTargetRoles;

    private String configCategory;

    private String configGroup;

    private String configLevel;

    private String templateName;
    private String templateContent;
    private String displayName;
    private Integer heightMultiple;

    /**
     * 配置项所属的服务名称
     */
    private String serviceName;

    /**
     * 端口绑定的角色，多个角色用逗号分隔
     */
    private String bindRole;

    /**
     * 服务类型：NodePort或ClusterIP
     */
    private String serviceType;

    /**
     * 端口号
     */
    private String portNumber;

    /**
     * NodePort类型服务的外部端口号（30000-32767）
     */
    private String nodePort;

}
