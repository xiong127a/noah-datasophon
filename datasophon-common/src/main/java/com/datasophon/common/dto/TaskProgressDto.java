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

package com.datasophon.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 任务进度数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskProgressDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务状态 (RUNNING, COMPLETED, FAILED, CANCELLED)
     */
    private String status;

    /**
     * 进度百分比 (0-100)
     */
    private int progress;

    /**
     * 当前步骤描述
     */
    private String currentStep;

    /**
     * 总步骤数
     */
    private int totalSteps;

    /**
     * 已完成步骤数
     */
    private int completedSteps;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建成功的任务进度
     */
    public static TaskProgressDto success(String taskId, int progress, String currentStep, int completedSteps, int totalSteps) {
        return TaskProgressDto.builder()
                .taskId(taskId)
                .status("RUNNING")
                .progress(progress)
                .currentStep(currentStep)
                .completedSteps(completedSteps)
                .totalSteps(totalSteps)
                .build();
    }

    /**
     * 创建完成的任务进度
     */
    public static TaskProgressDto completed(String taskId) {
        return TaskProgressDto.builder()
                .taskId(taskId)
                .status("COMPLETED")
                .progress(100)
                .currentStep("任务完成")
                .endTime(new Date())
                .build();
    }

    /**
     * 创建失败的任务进度
     */
    public static TaskProgressDto failed(String taskId, String errorMessage) {
        return TaskProgressDto.builder()
                .taskId(taskId)
                .status("FAILED")
                .currentStep("任务失败")
                .errorMessage(errorMessage)
                .endTime(new Date())
                .build();
    }
}