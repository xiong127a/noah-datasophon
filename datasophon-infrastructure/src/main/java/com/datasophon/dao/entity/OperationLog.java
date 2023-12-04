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

package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

//@TableName("t_ddh_operation_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Integer id;

    /**
     * 请求地址
     */
    private String url;

    /**
     * 客户端ip
     */
    private String ip;

    /**
     * 操作模块
     */
    private String operationModule;


    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 请求数据
     */
    private String paramAndValue;;


    //返回值
    private String returnValue;

    //返回状态码
    private Integer returnCode;

    //返回状态码
    private Integer returnMsg;

    //操作人
    private String operateUser;

    //操作时间
    private String startTime;

    //结束时间
    private String endTime;

    //操作耗时
    private Long costTime;


}
