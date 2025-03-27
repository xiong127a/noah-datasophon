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
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.sshd.client.session.ClientSession;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class HostInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer clusterId;

    private String ip;

    private String hostname;

    /**
     * 是否受管
     */
    private boolean managed;

    /**
     * 检测结果
     */
    private CheckResult checkResult;

    /**
     * 检查项列表
     */
    private List<CheckItem> checkItems;

    private String sshUser;

    private Integer sshPort;

    private String sshPassword;
    /**
     * 安装进度
     */
    private Integer progress;

    private String clusterCode;

    /**
     * 安装状态1:正在安装 2：安装成功 3：安装失败
     */
    private InstallState installState;

    private Integer installStateCode;

    private String errMsg;

    private String message;

    private Date createTime;

    private String cpuArchitecture;

    /**
     * 操作系统信息
     */
    private OsInfo osInfo;

    /**
     * 主机整体状态 - 枚举类型，与CheckItem.Status保持一致
     */
    private CheckItem.Status status;

    /**
     * 状态缓存是否失效 - 不序列化此字段
     */
    private transient boolean statusCacheDirty = true;

    /**
     * -- GETTER --
     * 是否使用已存在的会话（用于连接复用）
     * -- SETTER --
     * 设置是否使用已存在的会话
     * 
     */
    // 添加连接复用相关属性
    @Setter
    @Getter
    @JsonIgnore
    private transient boolean useExistingSession = false;

    /**
     * -- GETTER --
     * 获取外部会话
     * -- SETTER --
     * 设置外部会话
     * 
     */
    @Setter
    @Getter
    @JsonIgnore
    private transient ClientSession externalSession = null;

    public HostInfo(String ip, int sshPort, String sshUser) {
        this.ip = ip;
        this.sshPort = sshPort;
        this.sshUser = sshUser;
    }

    public HostInfo() {
    }

    /**
     * 会话是否准备就绪
     */
    public boolean isSessionReady() {
        return externalSession != null && externalSession.isOpen();
    }

    /**
     * 获取主机整体状态 - 枚举类型
     */
    public CheckItem.Status getStatus() {
        if (statusCacheDirty) {
            calculateStatus();
        }
        return status;
    }

    /**
     * 设置主机整体状态 - 枚举类型
     */
    public void setStatus(CheckItem.Status status) {
        this.status = status;
        // 当状态被手动设置时，缓存被视为有效
        this.statusCacheDirty = false;
    }

    /**
     * 获取用于前端展示的状态
     * 对于混合状态(部分成功部分跳过)的特殊处理，返回"MIXED"
     * 这样前端代码可以保持不变
     */
    public String getStatusStr() {
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
        boolean hasSkipped = false;
        boolean hasSuccess = false;
        for (CheckItem item : checkItems) {
            if (item.getStatus() == CheckItem.Status.SKIPPED) {
                hasSkipped = true;
            }
            if (item.getStatus() == CheckItem.Status.SUCCESS) {
                hasSuccess = true;
            }
            if (hasSkipped && hasSuccess) {
                return true;
            }
        }
        return false;
    }

    /**
     * 设置检查项列表
     * 重写此方法以在设置新的检查项时标记状态缓存为脏
     */
    public void setCheckItems(List<CheckItem> checkItems) {
        this.checkItems = checkItems;
        this.statusCacheDirty = true;
    }

    /**
     * 更新单个检查项的状态并自动计算主机状态
     * 这是推荐的更新检查项状态的方法，可以自动触发状态计算
     *
     * @param itemId    检查项ID
     * @param newStatus 新状态
     * @param message   状态消息
     * @return 状态是否发生变化
     */
    public boolean updateCheckItemStatus(Integer itemId, CheckItem.Status newStatus, String message) {
        if (checkItems == null) {
            return false;
        }

        boolean statusChanged = false;
        for (CheckItem item : checkItems) {
            if (item.getId().equals(itemId)) {
                if (item.getStatus() != newStatus) {
                    item.setStatus(newStatus);
                    if (message != null) {
                        item.setMessage(message);
                    }
                    statusChanged = true;
                }
                break;
            }
        }

        if (statusChanged) {
            statusCacheDirty = true;
            calculateStatus();
        }

        return statusChanged;
    }

    /**
     * 批量更新检查项状态
     * 对于需要同时更新多个检查项的场景，性能更优
     *
     * @param updates 检查项ID与新状态的映射
     * @return 状态是否发生变化
     */
    public boolean batchUpdateCheckItems(Map<Integer, CheckItem.Status> updates) {
        if (checkItems == null || updates == null || updates.isEmpty()) {
            return false;
        }

        boolean anyChange = false;
        for (CheckItem item : checkItems) {
            if (updates.containsKey(item.getId())) {
                CheckItem.Status newStatus = updates.get(item.getId());
                if (item.getStatus() != newStatus) {
                    item.setStatus(newStatus);
                    anyChange = true;
                }
            }
        }

        if (anyChange) {
            statusCacheDirty = true;
            calculateStatus();
        }

        return anyChange;
    }

    /**
     * 计算主机的整体状态
     * 状态计算规则：
     * 1. 如果有任何检查项正在检查中，则状态为CHECKING
     * 2. 如果有任何检查项失败，则状态为FAILED
     * 3. 如果所有检查项都成功，则状态为SUCCESS
     * 4. 如果有等待检查的项目，则状态为WAITING
     * 5. 如果所有项目都被跳过，则状态为SKIPPED
     * 
     * 该方法同时设置status和checkResult字段，确保两者一致
     */
    public void calculateStatus() {
        if (checkItems == null || checkItems.isEmpty()) {
            this.status = CheckItem.Status.WAITING;
            // 没有检查项或检查项为空时，设置为等待检查
            this.checkResult = new CheckResult(9999, "等待主机校验");
            this.statusCacheDirty = false;
            return;
        }

        boolean hasChecking = false;
        boolean hasFailed = false;
        boolean hasWaiting = false;
        boolean hasSkipped = false;
        boolean hasSuccess = false;

        // 统计检查项的状态
        int total = checkItems.size();
        int successCount = 0;
        int failedCount = 0;
        int waitingCount = 0;
        int checkingCount = 0;
        int skippedCount = 0;

        for (CheckItem item : checkItems) {
            CheckItem.Status itemStatus = item.getStatus();

            if (itemStatus == CheckItem.Status.CHECKING) {
                hasChecking = true;
                checkingCount++;
            } else if (itemStatus == CheckItem.Status.FAILED) {
                hasFailed = true;
                failedCount++;
            } else if (itemStatus == CheckItem.Status.WAITING) {
                hasWaiting = true;
                waitingCount++;
            } else if (itemStatus == CheckItem.Status.SKIPPED) {
                hasSkipped = true;
                skippedCount++;
            } else if (itemStatus == CheckItem.Status.SUCCESS) {
                hasSuccess = true;
                successCount++;
            }
        }

        // 根据检查项状态计算主机整体状态
        if (hasChecking) {
            this.status = CheckItem.Status.CHECKING;
            // 检查中，提供更详细的信息
            this.checkResult = new CheckResult(10000,
                    String.format("开始主机校验：进行中(%d/%d)，已通过(%d)，已失败(%d)，已跳过(%d)",
                            checkingCount, total, successCount, failedCount, skippedCount));
        } else if (hasFailed) {
            this.status = CheckItem.Status.FAILED;
            // 主机校验不通过，提供失败数量
            this.checkResult = new CheckResult(10043,
                    String.format("主机校验不通过：%d个检查项未通过，%d个检查项通过",
                            failedCount, successCount));
        } else if (hasWaiting) {
            this.status = CheckItem.Status.WAITING;
            // 等待检查，提供等待检查的数量
            this.checkResult = new CheckResult(9999,
                    String.format("等待主机校验：%d个检查项待检查", waitingCount));
        } else if (hasSuccess && !hasSkipped) {
            this.status = CheckItem.Status.SUCCESS;
            // 主机校验成功
            this.checkResult = new CheckResult(10001,
                    String.format("主机校验成功：全部%d个检查项通过", successCount));
        } else if (skippedCount == total) {
            this.status = CheckItem.Status.SKIPPED;
            // 全部检查项都已跳过
            this.checkResult = new CheckResult(10044, "主机校验已跳过：所有检查项已跳过");
        } else if (hasSuccess && hasSkipped && !hasFailed) {
            // 添加新条件：部分检查项通过，部分跳过，没有失败项
            this.status = CheckItem.Status.SUCCESS; // 仍然使用SUCCESS状态
            this.checkResult = new CheckResult(10001,
                    String.format("主机校验成功：%d个检查项通过，%d个检查项已跳过",
                            successCount, skippedCount));
        } else {
            // 部分检查项已跳过部分通过，仍使用SKIPPED状态，但前端可通过getStatusStr()获取"MIXED"
            this.status = CheckItem.Status.SKIPPED;
            this.checkResult = new CheckResult(10043,
                    String.format("主机校验不完整：已通过(%d)，已失败(%d)，已跳过(%d)",
                            successCount, failedCount, skippedCount));
        }

        // 状态计算完成，标记缓存为有效
        this.statusCacheDirty = false;
    }

}
