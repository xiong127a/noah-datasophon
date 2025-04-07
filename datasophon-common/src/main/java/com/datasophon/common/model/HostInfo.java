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

    @Setter
    private String hostname;

    /**
     * 完全限定域名(FQDN)
     */
    @Setter
    private String fqdn;

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
     * 操作系统信息收集状态
     * 可能的值: LOADING, SUCCESS, ERROR
     */
    private OsInfoStatusEnum osInfoStatus;

    /**
     * SSH连接状态
     * 可能的值: SUCCESS, ERROR, LOADING
     */
    private OsInfoStatusEnum sshConnectStatus;

    /**
     * SSH连接错误信息
     * 当SSH连接失败时，存储详细的错误原因
     */
    private String sshErrorMsg;

    /**
     * 操作系统信息收集错误信息
     * 当操作系统信息收集失败时，存储详细的错误原因
     */
    private String osErrorMsg;

    /**
     * 通用错误信息
     * 用于存储其他类型的错误或者简洁的错误摘要
     */
    private String errorMessage;

    /**
     * 硬件信息收集状态
     * 可能的值: LOADING, SUCCESS, ERROR
     */
    private OsInfoStatusEnum hardwareStatus;

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
     * 1. 如果主机状态已经是FIXING或WAITING_FIX，保持不变（手动设置的状态优先）
     * 2. 如果有任何检查项正在检查中，则状态为CHECKING
     * 3. 如果有任何检查项失败，则状态为FAILED
     * 4. 如果所有检查项都成功，则状态为SUCCESS
     * 5. 如果有等待检查的项目，则状态为WAITING
     * 6. 如果所有项目都被跳过，则状态为SKIPPED
     *
     * 该方法同时设置status和checkResult字段，确保两者一致
     */
    public void calculateStatus() {
        // 如果状态是手动设置的修复中或等待修复状态，保持不变
        if (this.status == CheckItem.Status.FIXING || this.status == CheckItem.Status.WAITING_FIX) {
            String statusName = this.status == CheckItem.Status.FIXING ? "修复中" : "等待修复";
            if (this.status == CheckItem.Status.WAITING_FIX) {
                // 如果是等待修复状态，更新checkResult以反映等待修复
                this.checkResult = new CheckResult(10045, "等待修复：等待修复失败的检查项");
                this.statusCacheDirty = false;
                return;
            } else if (this.status == CheckItem.Status.FIXING) {
                // 如果是修复中状态，更新checkResult以反映正在修复
                this.checkResult = new CheckResult(10046, "修复进行中：正在修复失败的检查项");
                this.statusCacheDirty = false;
                return;
            }
        }

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
        boolean hasFixing = false;

        // 统计检查项的状态
        int total = checkItems.size();
        int successCount = 0;
        int failedCount = 0;
        int waitingCount = 0;
        int checkingCount = 0;
        int skippedCount = 0;
        int fixingCount = 0;

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
            } else if (itemStatus == CheckItem.Status.FIXING) {
                hasFixing = true;
                fixingCount++;
            }
        }

        // 根据检查项状态计算主机整体状态
        if (hasFixing) {
            this.status = CheckItem.Status.FIXING;
            // 修复中，提供更详细的信息
            this.checkResult = new CheckResult(10046,
                    String.format("修复进行中：正在修复%d个检查项", fixingCount));
        } else if (hasChecking) {
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

    /**
     * 获取主机名状态
     */
    public OsInfoStatusEnum getHostnameStatus() {
        // 首先检查osInfo对象是否存在
        if (osInfo != null && osInfo.getHostnameStatus() != null) {
            return osInfo.getHostnameStatus();
        }
        // 如果osInfo为空或其中没有状态信息，返回LOADING
        return OsInfoStatusEnum.LOADING;
    }

    /**
     * 设置主机名状态
     */
    public void setHostnameStatus(OsInfoStatusEnum status) {
        // 如果osInfo存在，设置osInfo中的状态
        if (osInfo != null) {
            osInfo.setHostnameStatus(status);
        } else {
            // 如果osInfo不存在，创建新的OsInfo对象
            osInfo = new OsInfo();
            osInfo.setHostnameStatus(status);
        }
    }

    /**
     * 获取操作系统状态
     */
    public OsInfoStatusEnum getOsStatus() {
        // 首先检查osInfo对象是否存在
        if (osInfo != null && osInfo.getOsStatus() != null) {
            return osInfo.getOsStatus();
        }
        // 如果osInfo为空或其中没有状态信息，返回LOADING
        return OsInfoStatusEnum.LOADING;
    }

    /**
     * 设置操作系统状态
     */
    public void setOsStatus(OsInfoStatusEnum status) {
        // 如果osInfo存在，设置osInfo中的状态
        if (osInfo != null) {
            osInfo.setOsStatus(status);
        } else {
            // 如果osInfo不存在，创建新的OsInfo对象
            osInfo = new OsInfo();
            osInfo.setOsStatus(status);
        }
    }

    /**
     * 获取DNS状态
     */
    public OsInfoStatusEnum getDnsStatus() {
        // 首先检查osInfo对象是否存在
        if (osInfo != null && osInfo.getDnsStatus() != null) {
            return osInfo.getDnsStatus();
        }
        // 如果osInfo为空或其中没有状态信息，返回LOADING
        return OsInfoStatusEnum.LOADING;
    }

    /**
     * 设置DNS状态
     */
    public void setDnsStatus(OsInfoStatusEnum status) {
        // 如果osInfo存在，设置osInfo中的状态
        if (osInfo != null) {
            osInfo.setDnsStatus(status);
        } else {
            // 如果osInfo不存在，创建新的OsInfo对象
            osInfo = new OsInfo();
            osInfo.setDnsStatus(status);
        }
    }

    /**
     * 获取CPU状态
     */
    public OsInfoStatusEnum getCpuStatus() {
        // 首先检查osInfo对象是否存在
        if (osInfo != null && osInfo.getCpuStatus() != null) {
            return osInfo.getCpuStatus();
        }
        // 如果osInfo为空或其中没有状态信息，返回LOADING
        return OsInfoStatusEnum.LOADING;
    }

    /**
     * 设置CPU状态
     */
    public void setCpuStatus(OsInfoStatusEnum status) {
        // 如果osInfo存在，设置osInfo中的状态
        if (osInfo != null) {
            osInfo.setCpuStatus(status);
        } else {
            // 如果osInfo不存在，创建新的OsInfo对象
            osInfo = new OsInfo();
            osInfo.setCpuStatus(status);
        }
    }

    /**
     * 获取内存状态
     */
    public OsInfoStatusEnum getMemoryStatus() {
        // 首先检查osInfo对象是否存在
        if (osInfo != null && osInfo.getMemoryStatus() != null) {
            return osInfo.getMemoryStatus();
        }
        // 如果osInfo为空或其中没有状态信息，返回LOADING
        return OsInfoStatusEnum.LOADING;
    }

    /**
     * 设置内存状态
     */
    public void setMemoryStatus(OsInfoStatusEnum status) {
        // 如果osInfo存在，设置osInfo中的状态
        if (osInfo != null) {
            osInfo.setMemoryStatus(status);
        } else {
            // 如果osInfo不存在，创建新的OsInfo对象
            osInfo = new OsInfo();
            osInfo.setMemoryStatus(status);
        }
    }

    /**
     * 获取磁盘状态
     */
    public OsInfoStatusEnum getDiskStatus() {
        // 首先检查osInfo对象是否存在
        if (osInfo != null && osInfo.getDiskStatus() != null) {
            return osInfo.getDiskStatus();
        }
        // 如果osInfo为空或其中没有状态信息，返回LOADING
        return OsInfoStatusEnum.LOADING;
    }

    /**
     * 设置磁盘状态
     */
    public void setDiskStatus(OsInfoStatusEnum status) {
        // 如果osInfo存在，设置osInfo中的状态
        if (osInfo != null) {
            osInfo.setDiskStatus(status);
        } else {
            // 如果osInfo不存在，创建新的OsInfo对象
            osInfo = new OsInfo();
            osInfo.setDiskStatus(status);
        }
    }

    /**
     * 获取交换空间状态
     */
    public OsInfoStatusEnum getSwapStatus() {
        // 首先检查osInfo对象是否存在
        if (osInfo != null && osInfo.getSwapStatus() != null) {
            return osInfo.getSwapStatus();
        }
        // 如果osInfo为空或其中没有状态信息，返回LOADING
        return OsInfoStatusEnum.LOADING;
    }

    /**
     * 设置交换空间状态
     */
    public void setSwapStatus(OsInfoStatusEnum status) {
        // 如果osInfo存在，设置osInfo中的状态
        if (osInfo != null) {
            osInfo.setSwapStatus(status);
        } else {
            // 如果osInfo不存在，创建新的OsInfo对象
            osInfo = new OsInfo();
            osInfo.setSwapStatus(status);
        }
    }

    /**
     * 获取GPU状态
     */
    public OsInfoStatusEnum getGpuStatus() {
        // 首先检查osInfo对象是否存在
        if (osInfo != null && osInfo.getGpuStatus() != null) {
            return osInfo.getGpuStatus();
        }
        // 如果osInfo为空或其中没有状态信息，返回LOADING
        return OsInfoStatusEnum.LOADING;
    }

    /**
     * 设置GPU状态
     */
    public void setGpuStatus(OsInfoStatusEnum status) {
        // 如果osInfo存在，设置osInfo中的状态
        if (osInfo != null) {
            osInfo.setGpuStatus(status);
        } else {
            // 如果osInfo不存在，创建新的OsInfo对象
            osInfo = new OsInfo();
            osInfo.setGpuStatus(status);
        }
    }

    /**
     * 获取网络状态
     */
    public OsInfoStatusEnum getNetworkStatus() {
        // 首先检查osInfo对象是否存在
        if (osInfo != null && osInfo.getNetworkStatus() != null) {
            return osInfo.getNetworkStatus();
        }
        // 如果osInfo为空或其中没有状态信息，返回LOADING
        return OsInfoStatusEnum.LOADING;
    }

    /**
     * 设置网络状态
     */
    public void setNetworkStatus(OsInfoStatusEnum status) {
        // 如果osInfo存在，设置osInfo中的状态
        if (osInfo != null) {
            osInfo.setNetworkStatus(status);
        } else {
            // 如果osInfo不存在，创建新的OsInfo对象
            osInfo = new OsInfo();
            osInfo.setNetworkStatus(status);
        }
    }

    public OsInfoStatusEnum getOsInfoStatus() {
        return osInfoStatus;
    }

    public void setOsInfoStatus(OsInfoStatusEnum status) {
        this.osInfoStatus = status;
    }

    public OsInfoStatusEnum getSshConnectStatus() {
        return sshConnectStatus;
    }

    public void setSshConnectStatus(OsInfoStatusEnum status) {
        this.sshConnectStatus = status;
    }
}
