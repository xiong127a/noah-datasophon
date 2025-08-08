/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasophon.common.converter;

import com.datasophon.common.enums.ServiceType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * 字符串到ServiceType枚举的转换器
 * 用于Spring Boot @RequestParam参数自动转换
 * 
 * @author 任相鹏
 * @email 635887935@qq.com  
 * @date 2025-01-20
 */
@Component
public class StringToServiceTypeConverter implements Converter<String, ServiceType> {
    
    @Override
    public ServiceType convert(String source) {
        try {
            return ServiceType.fromCode(source);
        } catch (IllegalArgumentException e) {
            // 转换失败时，Spring Boot会自动返回400错误，这里重新抛出异常让Spring处理
            throw new IllegalArgumentException("无效的服务类型参数: " + source + "，支持的值: core, custom", e);
        }
    }
}
