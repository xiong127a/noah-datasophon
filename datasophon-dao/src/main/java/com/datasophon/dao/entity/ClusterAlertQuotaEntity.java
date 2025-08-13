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

import com.datasophon.common.enums.AlertLevel;
import com.datasophon.common.enums.QuotaState;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import lombok.Data;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;

@Data
@Table("t_ddh_cluster_alert_quota")
public class ClusterAlertQuotaEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


    /**
     * 告警指标名称
     */
    private String alertQuotaName;
    /**
     * 服务分类
     */
    private String serviceCategory;
    /**
     * 告警指标表达式
     */
    private String alertExpr;
    /**
     * 告警级别 1:警告2：异常
     */
    private AlertLevel alertLevel;
    /**
     * 告警组
     */
    private Long alertGroupId;
    /**
     * 通知组
     */
    private Long noticeGroupId;
    /**
     * 告警建议
     */
    private String alertAdvice;
    /**
     * 比较方式 !=;>;<
     */
    private String compareMethod;
    /**
     * 告警阀值
     */
    private Long alertThreshold;
    /**
     * 告警策略 1:单次2：连续
     */
    private Integer alertTactic;
    /**
     * 间隔时长 单位分钟
     */
    private Integer intervalDuration;
    /**
     * 触发时长 单位秒
     */
    private Integer triggerDuration;

    private String serviceRoleName;

    private QuotaState quotaState;



    @Column(ignore = true)
    private Integer quotaStateCode;

    @Column(ignore = true)
    private String alertGroupName;

    @Column(ignore = true)
    private String noticeGroupName;

}
