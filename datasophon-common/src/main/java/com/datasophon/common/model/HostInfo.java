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

import com.datasophon.common.enums.InstallState;
import com.datasophon.common.enums.OsInfoStatusEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 主机信息管理模型
 * 专注于主机管理、验证检查、安装状态等核心职责
 * 
 * 设计原则：
 * 1. 单一职责 - 专注主机管理和状态跟踪
 * 2. 组合模式 - 通过组合OsInfo获取系统详细信息
 * 3. 状态隔离 - 主机验证状态与系统信息状态分离
 * 4. 现代特性 - 使用JDK21特性提升代码质量
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-28
 */
@Data
public class HostInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ==================== 主机基本标识信息 ====================
    
    /**
     * 集群ID
     */
    private Long clusterId;
    
    /**
     * 集群代码
     */
    private String clusterCode;

    /**
     * 主机IP地址（主键标识）
     */
    private String ip;

    /**
     * 主机名
     */
    private String hostname;

    /**
     * 完全限定域名(FQDN)
     */
    private String fqdn;

    /**
     * CPU架构信息
     */
    private String cpuArchitecture;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    // ==================== SSH连接配置 ====================

    /**
     * SSH用户名
     */
    private String sshUser;

    /**
     * SSH端口
     */
    private Integer sshPort;

    /**
     * SSH密码
     */
    private String sshPassword;

    // ==================== 主机管理状态 ====================

    /**
     * 是否受管
     */
    private Boolean managed;
    
    /**
     * 主机整体验证状态
     */
    private CheckItem.Status status;

    /**
     * 验证检查结果摘要
     */
    private CheckResult checkResult;

    /**
     * 检查项列表
     */
    private List<CheckItem> checkItems;

    // ==================== SSH连接状态 ====================
    
    /**
     * SSH连接状态
     */
    private OsInfoStatusEnum sshConnectStatus;

    /**
     * SSH连接错误信息
     */
    private String sshErrorMsg;

    // ==================== 安装管理 ====================

    /**
     * 安装状态
     */
    private InstallState installState;

    /**
     * 安装状态代码（向后兼容）
     */
    private Integer installStateCode;

    /**
     * 安装进度 (0-100)
     */
    private Integer progress;

    // ==================== 系统信息组合 ====================

    /**
     * 操作系统详细信息
     * 使用组合模式，不代理内部状态
     */
    private OsInfo osInfo;

    // ==================== 错误和消息管理 ====================

    /**
     * 主要错误消息
     */
    private String errMsg;

    /**
     * 状态消息
     */
    private String message;

    /**
     * 通用错误信息
     */
    private String errorMessage;

    // ==================== 构造函数 ====================
    
    /**
     * 兼容性构造函数
     */
    public HostInfo(String ip, int sshPort, String sshUser) {
        this();
        this.ip = ip;
        this.sshPort = sshPort;
        this.sshUser = sshUser;
    }

    /**
     * 默认构造函数
     */
    public HostInfo() {
        this.sshPort = 22;
        this.managed = false;
        this.status = CheckItem.Status.WAITING;
        this.sshConnectStatus = OsInfoStatusEnum.LOADING;
        this.installState = InstallState.RUNNING;
        this.progress = 0;
        this.createTime = LocalDateTime.now();
    }

    // ==================== 状态管理工具方法 ====================

    /**
     * 获取主机整体状态（触发状态计算）
     */
    public CheckItem.Status getStatus() {
        calculateStatus();
        return status;
    }

    /**
     * 获取用于前端展示的状态
     * 对于混合状态(部分成功部分跳过)的特殊处理，返回"MIXED"
     */
    public String getStatusStr() {
        calculateStatus();
        if (status == CheckItem.Status.SKIPPED && hasMixedItems()) {
            return "MIXED";
        }
        return status != null ? status.name() : null;
    }

    /**
     * 检查是否是混合状态（部分成功部分跳过）
     */
    private boolean hasMixedItems() {
        if (checkItems == null || checkItems.isEmpty()) {
            return false;
        }
        boolean hasSkipped = checkItems.stream()
                .anyMatch(item -> item.getStatus() == CheckItem.Status.SKIPPED);
        boolean hasSuccess = checkItems.stream()
                .anyMatch(item -> item.getStatus() == CheckItem.Status.SUCCESS);
        return hasSkipped && hasSuccess;
    }

    /**
     * 更新单个检查项的状态并自动计算主机状态
     *
     * @param itemId    检查项ID
     * @param newStatus 新状态
     * @param message   状态消息
     * @return 状态是否发生变化
     */
    public boolean updateCheckItemStatus(Long itemId, CheckItem.Status newStatus, String message) {
        if (checkItems == null) {
            return false;
        }

        boolean statusChanged = checkItems.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .map(item -> {
                    boolean changed = item.getStatus() != newStatus;
                    if (changed) {
                        item.setStatus(newStatus);
                        if (message != null) {
                            item.setMessage(message);
                        }
                    }
                    return changed;
                })
                .orElse(false);

        if (statusChanged) {
            calculateStatus();
        }

        return statusChanged;
    }

    /**
     * 批量更新检查项状态
     *
     * @param updates 检查项ID与新状态的映射
     */
    public void batchUpdateCheckItems(Map<Long, CheckItem.Status> updates) {
        if (checkItems == null || updates == null || updates.isEmpty()) {
            return;
        }

        boolean anyChange = checkItems.stream()
                .anyMatch(item -> {
                    Long itemId = item.getId();
                    CheckItem.Status newStatus = updates.get(itemId);
                    if (newStatus != null && item.getStatus() != newStatus) {
                        item.setStatus(newStatus);
                        return true;
                    }
                    return false;
                });

        if (anyChange) {
            calculateStatus();
        }
    }

    /**
     * 计算主机的整体状态
     * 
     * 状态计算规则：
     * 1. 手动设置的修复状态优先（FIXING/WAITING_FIX）
     * 2. 有检查中项目 → CHECKING
     * 3. 有失败项目 → FAILED  
     * 4. 全部成功 → SUCCESS
     * 5. 有等待项目 → WAITING
     * 6. 全部跳过 → SKIPPED
     */
    public void calculateStatus() {
        // 保护手动设置的修复状态
        if (this.status == CheckItem.Status.FIXING || this.status == CheckItem.Status.WAITING_FIX) {
            updateCheckResultForFixingStatus();
            return;
        }

        if (checkItems == null || checkItems.isEmpty()) {
            this.status = CheckItem.Status.WAITING;
            this.checkResult = new CheckResult(9999, "等待主机校验");
            return;
        }

        // 统计各种状态的数量
        var statusCounts = checkItems.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        CheckItem::getStatus,
                        java.util.stream.Collectors.counting()
                ));

        long total = checkItems.size();
        long checkingCount = statusCounts.getOrDefault(CheckItem.Status.CHECKING, 0L);
        long failedCount = statusCounts.getOrDefault(CheckItem.Status.FAILED, 0L);
        long successCount = statusCounts.getOrDefault(CheckItem.Status.SUCCESS, 0L);
        long waitingCount = statusCounts.getOrDefault(CheckItem.Status.WAITING, 0L);
        long skippedCount = statusCounts.getOrDefault(CheckItem.Status.SKIPPED, 0L);
        long fixingCount = statusCounts.getOrDefault(CheckItem.Status.FIXING, 0L);

        // 根据优先级计算状态
        if (fixingCount > 0) {
            this.status = CheckItem.Status.FIXING;
            this.checkResult = new CheckResult(10046, 
                    String.format("修复进行中：正在修复%d个检查项", fixingCount));
        } else if (checkingCount > 0) {
            this.status = CheckItem.Status.CHECKING;
            this.checkResult = new CheckResult(10000,
                    String.format("主机校验进行中：%d/%d项检查中", checkingCount, total));
        } else if (failedCount > 0) {
            this.status = CheckItem.Status.FAILED;
            this.checkResult = new CheckResult(10043,
                    String.format("主机校验失败：%d项未通过，%d项通过", failedCount, successCount));
        } else if (waitingCount > 0) {
            this.status = CheckItem.Status.WAITING;
            this.checkResult = new CheckResult(9999,
                    String.format("等待主机校验：%d项待检查", waitingCount));
        } else if (successCount == total) {
            this.status = CheckItem.Status.SUCCESS;
            this.checkResult = new CheckResult(10001,
                    String.format("主机校验成功：全部%d项检查通过", total));
        } else if (skippedCount == total) {
            this.status = CheckItem.Status.SKIPPED;
            this.checkResult = new CheckResult(10044, "主机校验已跳过：所有检查项已跳过");
        } else {
            // 混合状态
            this.status = CheckItem.Status.SUCCESS;
            this.checkResult = new CheckResult(10001,
                    String.format("主机校验完成：%d项通过，%d项跳过", successCount, skippedCount));
        }
    }

    /**
     * 更新修复状态的检查结果
     */
    private void updateCheckResultForFixingStatus() {
        if (this.status == CheckItem.Status.WAITING_FIX) {
            this.checkResult = new CheckResult(10045, "等待修复：等待修复失败的检查项");
        } else {
            this.checkResult = new CheckResult(10046, "修复进行中：正在修复失败的检查项");
        }
    }

    // ==================== 系统信息访问方法 ====================
    
    /**
     * 获取操作系统信息收集状态
     * 优先从OsInfo获取，如果不存在则返回LOADING
     */
    public OsInfoStatusEnum getOsInfoStatus() {
        return osInfo != null ? osInfo.getOverallStatus() : OsInfoStatusEnum.LOADING;
    }
    
    /**
     * 检查系统信息是否收集完成
     */
    public boolean isSystemInfoComplete() {
        return osInfo != null && osInfo.isCollectionComplete();
    }
    
    /**
     * 获取系统信息收集进度
     */
    public int getSystemInfoProgress() {
        return osInfo != null ? osInfo.getCollectionProgress() : 0;
    }

}
