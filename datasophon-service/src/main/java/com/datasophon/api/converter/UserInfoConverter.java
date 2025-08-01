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

package com.datasophon.api.converter;


import com.datasophon.common.converter.BaseConverter;
import com.datasophon.common.dto.UserInfoDTO;
import com.datasophon.common.vo.UserInfoVO;
import com.datasophon.dao.entity.UserInfoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 用户信息转换器
 * 实现Entity、DTO、VO之间的相互转换
 * 放置在API层，因为API层可以访问所有下层的类
 * 
 * @author DataSophon
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
@Component
public interface UserInfoConverter extends BaseConverter<UserInfoEntity, UserInfoDTO, UserInfoVO> {

    /**
     * Entity转DTO - 直接映射，字段名相同
     */
    @Override
    UserInfoDTO entityToDto(UserInfoEntity entity);

    /**
     * DTO转Entity - 直接映射，字段名相同
     */
    @Override
    UserInfoEntity dtoToEntity(UserInfoDTO dto);

    /**
     * Entity转VO - 需要特殊处理的字段
     */
    @Override
    @Mapping(target = "createTime", source = "createTime", qualifiedByName = "dateToString")
    @Mapping(target = "lastLoginTime", source = "lastLoginTime", qualifiedByName = "dateToString")
    @Mapping(target = "userTypeDesc", source = "userType", qualifiedByName = "userTypeToDesc")
    @Mapping(target = "email", source = "email", qualifiedByName = "maskEmail")
    @Mapping(target = "phone", source = "phone", qualifiedByName = "maskPhone")
    @Mapping(target = "online", constant = "false") // 默认离线，实际使用时需要从Redis等获取
    UserInfoVO entityToVo(UserInfoEntity entity);

    /**
     * DTO转VO - 需要特殊处理的字段
     */
    @Override
    @Mapping(target = "createTime", source = "createTime", qualifiedByName = "dateToString")
    @Mapping(target = "lastLoginTime", source = "lastLoginTime", qualifiedByName = "dateToString")
    @Mapping(target = "userTypeDesc", source = "userType", qualifiedByName = "userTypeToDesc")
    @Mapping(target = "email", source = "email", qualifiedByName = "maskEmail")
    @Mapping(target = "phone", source = "phone", qualifiedByName = "maskPhone")
    @Mapping(target = "online", constant = "false")
    UserInfoVO dtoToVo(UserInfoDTO dto);

    /**
     * 日期转字符串
     */
    @Named("dateToString")
    default String dateToString(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    /**
     * 用户类型转描述
     */
    @Named("userTypeToDesc")
    default String userTypeToDesc(Integer userType) {
        if (userType == null) {
            return "未知";
        }
        return switch (userType) {
            case 1 -> "管理员";
            case 2 -> "普通用户";
            default -> "未知";
        };
    }

    /**
     * 邮箱脱敏
     */
    @Named("maskEmail")
    default String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) {
            return email;
        }
        String prefix = email.substring(0, 1);
        String suffix = email.substring(atIndex);
        return prefix + "***" + suffix;
    }

    /**
     * 手机号脱敏
     */
    @Named("maskPhone")
    default String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}