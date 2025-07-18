<!--
 *
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
 *
 */
-->

<!-- @describe: step2-主机环境校验 -->
<template>
  <div class="steps2 steps">
    <!-- K8S模式 - 简化的主机校验 -->
    <template v-if="depType === 'Kubernetes'">
      <div class="k8s-host-check">
        <div class="hero-section">
          <h1 class="hero-title">Kubernetes主机校验</h1>
          <p class="hero-subtitle">验证Kubernetes集群中的主机状态，确保可以正常部署服务</p>
        </div>

        <div class="k8s-hosts-container">
          <div class="hosts-table-wrapper">
            <a-table
              :columns="k8sColumns"
              :loading="loading"
              :dataSource="dataSource"
              :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
              rowKey="ip"
              :pagination="pagination"
              class="k8s-host-table"
            >
              <template slot="status" slot-scope="text, record">
                <div class="status-indicator">
                  <a-icon 
                    :type="!record.managed ? 'check-circle' : 'exclamation-circle'"
                    :style="{ 
                      color: !record.managed ? '#52c41a' : '#ff4d4f',
                      fontSize: '16px',
                      marginRight: '8px'
                    }"
                  />
                  <span :style="{ 
                    color: !record.managed ? '#52c41a' : '#ff4d4f',
                    fontWeight: '500'
                  }">
                    {{ record.managed ? '已受管' : '未受管' }}
                  </span>
                </div>
              </template>
            </a-table>
          </div>

          <div class="k8s-summary">
            <div class="summary-card">
              <div class="summary-header">
                <a-icon type="info-circle" class="summary-icon" />
                <span class="summary-title">校验结果</span>
              </div>
              <div class="summary-content">
                <div class="summary-item">
                  <span class="item-label">总主机数：</span>
                  <span class="item-value">{{ dataSource.length }}</span>
                </div>
                <div class="summary-item">
                  <span class="item-label">已受管：</span>
                  <span class="item-value error">{{ managedCount }}</span>
                </div>
                <div class="summary-item">
                  <span class="item-label">未受管：</span>
                  <span class="item-value success">{{ unmanagedCount }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- PVM模式 - 完整的主机校验 -->
    <template v-else>
    <!-- 添加OsFloatingCard组件的使用 -->
    <os-floating-card v-if="false"></os-floating-card>
    
    <div class="hero-section">
      <h1 class="hero-title">主机环境校验</h1>
      <p class="hero-subtitle">验证主机环境配置，确保系统顺利部署</p>

      <div class="queue-status-area">
        <queue-status-indicator :queue-status="queueStatus" />
      </div>
    </div>

    <div class="hosts-table-container">
      <!-- 使用TableOperations组件 -->
      <table-operations 
        :is-checking-active="isCheckingActive"
        :has-started-check="hasStartedCheck"
        :has-failed-items="hasFailedItems"
        @check-action="handleCheckAction"
        @set-hostname="showHostnameSettingModal"
        @sync-hosts="showSyncHostsModal"
        @fix-all-failed="fixAllFailedItems"
        @skip-all-failed="skipAllFailedItems"
      />
      <a-table
          @change="tableChange"
          :columns="columns"
          :loading="loading"
          :dataSource="dataSource"
          :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
          rowKey="ip"
          :pagination="pagination"
          expandable
          :expandedRowRender="expandedRowRender"
          :expandIcon="customExpandIcon"
          :class="'host-check-table'"
      >
      </a-table>
    </div>

    <!-- 使用新的AppleLogViewer组件 -->
    <apple-log-viewer
      :clusterId="clusterId"
      :visible.sync="logVisible"
      :title="logModalTitle"
      :hostIp="currentLogIp"
      :itemId="currentLogItemId"
      :itemName="currentLogItemName"
      @close="closeLogModal"
    />

    <!-- 使用FixConfirmModal组件 -->
    <fix-confirm-modal
      :visible.sync="fixConfirmVisible"
      :title="fixConfirmTitle"
      :content="fixConfirmContent"
      :loading="fixConfirmLoading"
      @cancel="handleFixCancel"
      @confirm="handleFixConfirm"
    />

    <!-- 使用HostnameEditModal组件 -->
    <hostname-edit-modal
      :visible="hostnameEditVisible"
      :loading="editLoading"
      :host="currentEditHost || {}"
      @cancel="cancelHostnameEdit"
      @submit="submitHostnameEdit"
    />

    <!-- 主机名批量设置弹窗 -->
    <hostname-setting-modal
      :visible="hostnameSettingVisible"
      :clusterId="clusterId"
      @close="closeHostnameSettingModal"
      @success="handleHostnameSettingSuccess"
      @syncHosts="showSyncHostsModal"
    />
    
    <!-- 同步hosts文件弹窗 -->
    <sync-hosts-file-modal
      :visible="syncHostsVisible"
      :clusterId="clusterId"
      @close="closeSyncHostsModal"
      @success="handleSyncHostsSuccess"
    />
    </template>
  </div>
</template>

<script>
import QueueStatusIndicator from '@/components/steps/step2/QueueStatusIndicator.vue'
// 导入操作系统浮窗组件
import OsFloatingCard from '@/components/steps/step2/OsFloatingCard.vue';
// 导入HostnameFloatingCard组件
import HostnameFloatingCard from '@/components/steps/step2/HostnameFloatingCard.vue';
import AppleLogViewer from './AppleLogViewer.vue';
// 导入新组件
import TableOperations from './TableOperations.vue';
import FixConfirmModal from './FixConfirmModal.vue';
import HostnameEditModal from './HostnameEditModal.vue';
import HostCheckItems from './HostCheckItems.vue';
import HostnameSettingModal from './HostnameSettingModal.vue';
import SyncHostsFileModal from './SyncHostsFileModal.vue';
import HostCheckService from './HostCheckService';

export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  props: {
    steps1Data: Object,
    depType:String,
  },
  components: {
    QueueStatusIndicator,
    OsFloatingCard,
    /* eslint-disable-next-line vue/no-unused-components */
    HostnameFloatingCard,
    AppleLogViewer,
    TableOperations,
    FixConfirmModal,
    HostnameEditModal,
    /* eslint-disable-next-line vue/no-unused-components */
    HostCheckItems,
    HostnameSettingModal,
    SyncHostsFileModal
  },
  data() {
    return {
      loading: false,
      isRequesting: false,
      isCheckingActive: false, // 是否有检查正在进行中
      hasStartedCheck: false, // 是否已开始过检查
      dataSource: [],
      selectedRowKeys: [],
      pagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showSizeChanger: true,
        pageSizeOptions: ["10", "50", "100", "500", "1000"],
        showTotal: (total) => `共 ${total} 条`,
      },
      timer: null,
      firstDataLoaded: false,
      checkItemsMap: {}, // 存储每个主机的校验项
      checkItem: null, // 当前查看日志的检查项
      // 使用新的AppleLogViewer组件，不再需要日志相关变量
      queueStatus: {
        queueSize: 0,
        runningTasks: 0,
        processorThreadAlive: true
      }, // 队列状态信息
      // 添加确认弹窗相关状态
      fixConfirmVisible: false,
      fixConfirmLoading: false,
      fixConfirmTitle: '',
      fixConfirmContent: '',
      fixConfirmIp: '',
      fixConfirmItem: null,
      
      // K8S模式的表格列定义
      k8sColumns: [
        {
          title: "序号",
          key: "index",
          width: 60,
          customRender: (text, row, index) => {
            const h = this.$createElement;
            const displayIndex = parseInt(
                this.pagination.current === 1
                    ? index + 1
                    : index + 1 + this.pagination.pageSize * (this.pagination.current - 1)
            );
            return h('span', {}, [displayIndex]);
          },
        },
        {
          title: "主机名",
          key: "hostname",
          dataIndex: "hostname",
          width: 200,
          customRender: (text, record) => {
            const h = this.$createElement;
            return h('div', { class: 'hostname-column' }, [
              h('span', { class: 'hostname-text' }, [text || record.ip]),
              h('div', { class: 'hostname-ip' }, [record.ip])
            ]);
          },
        },
        {
          title: "IP地址",
          key: "ip",
          dataIndex: "ip",
          width: 150,
        },
        {
          title: "受管状态",
          key: "status",
          width: 120,
          scopedSlots: { customRender: 'status' }
        },
        {
          title: "备注",
          key: "note",
          dataIndex: "note",
          customRender: (text, record) => {
            const h = this.$createElement;
            if (!record.managed) {
              return h('span', { style: { color: '#52c41a', fontWeight: '500' } }, ['可正常部署']);
            } else {
              return h('span', { style: { color: '#ff4d4f', fontWeight: '500' } }, ['已被其他集群使用']);
            }
          },
        }
      ],
      
      columns: [
        {
          title: "序号",
          key: "index",
          width: 50,
          customRender: (text, row, index) => {
            const h = this.$createElement;
            const displayIndex = parseInt(
                this.pagination.current === 1
                    ? index + 1
                    : index + 1 + this.pagination.pageSize * (this.pagination.current - 1)
            );
            return h('span', {}, [displayIndex]);
          },
        },
        {
          title: "主机名",
          key: "hostname",
          dataIndex: "hostname",
          width: 200,
          customRender: (text, record) => {
            const h = this.$createElement;
            return h('div', { class: 'hostname-column' }, [
              // 添加主机名悬浮卡片
              h('a-tooltip', {
                props: {
                  placement: 'right',
                  arrowPointAtCenter: true,
                  overlayClassName: 'hostname-tooltip',
                  getPopupContainer: () => document.body
                }
              }, [
                h('span', { class: 'hostname-text' }, [text || record.ip]),
                h('div', { slot: 'title' }, [
                  h('hostname-floating-card', {
                    props: { host: record }
                  })
                  ])
                ]),
              h('div', { class: 'hostname-ip' }, [record.ip])
            ]);
          },
        },
        {
          title: "IP地址",
          key: "ip",
          dataIndex: "ip",
          width: 150,
        },
        {
          title: "操作系统",
          key: "os",
          dataIndex: "os",
          width: 120,
          customRender: (text, record) => {
            const h = this.$createElement;
            if (text) {
              return h('a-tooltip', {
                props: {
                  placement: 'top',
                  arrowPointAtCenter: true,
                  overlayClassName: 'os-tooltip',
                  getPopupContainer: () => document.body
                }
              }, [
                h('span', { class: 'os-text' }, [text]),
                h('div', { slot: 'title' }, [
                  h('os-floating-card', {
                    props: { host: record }
                    })
                ])
              ]);
            }
            return h('span', {}, ['-']);
          },
        },
        {
          title: "校验状态",
          key: "status",
          dataIndex: "status",
          width: 120,
          customRender: (text, record) => {
            const h = this.$createElement;
            const status = this.calculateHostStatus(record);
            let statusText = '';
            let statusColor = '';
            let statusIcon = '';
            
            switch (status) {
              case 'SUCCESS':
                statusText = '通过';
                statusColor = '#52c41a';
                statusIcon = 'check-circle';
                break;
              case 'FAILED':
                statusText = '未通过';
                statusColor = '#ff4d4f';
                statusIcon = 'close-circle';
                break;
              case 'CHECKING':
                statusText = '检查中';
                statusColor = '#1890ff';
                statusIcon = 'loading';
                break;
              case 'WAITING':
                statusText = '等待检查';
                statusColor = '#faad14';
                statusIcon = 'clock-circle';
                break;
              case 'SKIPPED':
                statusText = '已跳过';
                statusColor = '#8c8c8c';
                statusIcon = 'minus-circle';
                break;
              case 'MIXED':
                statusText = '部分通过';
                statusColor = '#fa8c16';
                statusIcon = 'exclamation-circle';
                break;
              default:
                statusText = '未检查';
                statusColor = '#d9d9d9';
                statusIcon = 'question-circle';
            }
            
            return h('div', { class: 'status-indicator' }, [
                h('a-icon', {
                props: { type: statusIcon },
                style: { color: statusColor, marginRight: '4px' }
                }),
              h('span', { style: { color: statusColor, fontWeight: '500' } }, [statusText])
                ]);
          },
        },
        {
          title: "操作",
          key: "action",
          width: 200,
          customRender: (text, record) => {
            const h = this.$createElement;
            const status = this.calculateHostStatus(record);

            return h('div', { class: 'action-buttons' }, [
              h('a-button', {
                props: { type: 'link', size: 'small' },
                on: { click: () => this.retryEnvironment(record) }
              }, ['重试']),
              h('a-button', {
                props: { type: 'link', size: 'small' },
                on: { click: () => this.viewLogs(record) }
              }, ['查看日志'])
            ]);
          },
        },
      ],
    };
  },
  
  computed: {
    // K8S模式下的统计信息
    managedCount() {
      return this.dataSource.filter(host => host.managed).length;
    },
    
    unmanagedCount() {
      return this.dataSource.filter(host => !host.managed).length;
    },
    
    hasFailedItems() {
      if (this.depType === 'Kubernetes') {
        // K8S模式下，已受管的主机才是失败项
        return this.managedCount > 0;
      }
      // PVM模式的失败项检查逻辑
      return this.dataSource.some(host => {
        const status = this.calculateHostStatus(host);
        return status === 'FAILED' || status === 'MIXED';
      });
    }
  },
  
  watch: {
    depType: {
      handler(newVal) {
        if (newVal === 'Kubernetes') {
          // K8S模式下不需要轮询
          if (this.timer) {
            clearInterval(this.timer);
            this.timer = null;
          }
        }
      },
      immediate: true
    }
  },
  
  mounted() {
    this.getEnvironmentList();
  },
  
  beforeDestroy() {
    if (this.timer) {
      clearInterval(this.timer);
    }
  },
  
  methods: {
    // 获取主机列表
    getEnvironmentList(flag = true) {
      if (!flag) this.loading = true;
      
      const params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
        clusterId: this.clusterId,
        ...this.steps1Data,
      };

      this.$axiosPost(global.API.analysisHostList, params)
          .then((res) => {
          this.loading = false;

          if (res.code === 200) {
                // 检查是否有主机已完成检查，用于判断是否已开始过检查
                if (res.data && res.data.length > 0) {
                  const hasCompletedChecks = res.data.some(host => {
                    // 只要有一个主机的检查项不是WAITING状态，说明已经开始过检查
                    const checkItems = host.checkItems || [];
                    return checkItems.length > 0 &&
                        checkItems.some(item => item.status !== 'WAITING');
                  });

                  if (hasCompletedChecks) {
                    this.hasStartedCheck = true;
                }
              }

              this.dataSource = res.data;
              this.pagination.total = res.total;

              // 保存队列状态信息
              if (res.queueStatus) {
                this.queueStatus = res.queueStatus;
              }

            // K8S模式下的特殊处理
            if (this.depType === 'Kubernetes') {
              let data = JSON.parse(JSON.stringify(res.data));
                data && data.forEach(e => {
                if (e.checkResult && e.checkResult.code == '10001') {
                  e['CheckResult'] = e.checkResult;
                  delete e.checkResult;
                  // 移除轮询保存，只在点击下一步时保存
                  }
              });
              
              // K8S模式下自动选中所有主机
              if (this.depType === 'Kubernetes') {
                // 默认选择所有主机
                const allHostIps = this.dataSource.map(host => host.ip);
                this.selectedRowKeys = allHostIps;
              }
            }
            }
          })
          .catch((error) => {
            console.error('请求失败:', error);
          })
          .finally(() => {
            this.loading = false;
            this.isRequesting = false;
          });
    },



    // K8S模式下的直接保存API（使用完整硬件信息）
    saveKubernetesHostDirectApi(kubernetesHosts) {
      this.$axiosJsonPost(global.API.saveKubernetesHostDirect + '?clusterId=' + this.clusterId, kubernetesHosts)
        .then((res) => {
        if (res.code === 200) {
            console.log('K8S主机直接保存成功');
        } else {
            console.warn('K8S主机直接保存失败:', res.msg);
        }
        })
        .catch((error) => {
          console.warn('K8S主机直接保存异常:', error);
        });
    },

    // 获取K8S模式下的完整硬件信息
    async getK8sHostsWithHardwareInfo() {
      try {
        // 从后端获取缓存的K8S完整硬件信息
        const res = await this.$axiosGet(global.API.getK8sHostsWithHardwareInfo + '?clusterId=' + this.clusterId);
        if (res.code === 200) {
          return res.data;
        } else {
          console.warn('获取K8S硬件信息失败:', res.msg);
          return [];
        }
      } catch (error) {
        console.warn('获取K8S硬件信息异常:', error);
        return [];
      }
    },

    // 获取所有校验成功的主机列表（K8S模式）
    getSuccessfulHosts() {
      if (this.depType === 'Kubernetes') {
        // 在K8S模式下，只要选中的主机且未被管理即可
        const successfulHosts = this.dataSource.filter(host => 
          this.selectedRowKeys.includes(host.ip) && 
          !host.managed
        );
        
        return successfulHosts;
      } else {
        // 传统模式下的逻辑保持不变
        return this.dataSource.filter(host => 
          this.selectedRowKeys.includes(host.ip) && 
          host.CheckResult && 
          host.CheckResult.code === '10001'
        );
      }
    },

    // 主机环境校验是否完成（K8S模式简化）
    async hostCheckCompleted(callback) {
      if (this.depType === 'Kubernetes') {
        // K8S模式下，只要有未受管的主机就算完成
        const unmanagedHosts = this.dataSource.filter(host => !host.managed);
        const result = {
          hostCheckCompleted: unmanagedHosts.length > 0,
          data: unmanagedHosts.length > 0 ? 'K8S主机校验完成' : '没有可用的未受管主机'
        };
        
        if (callback) {
          callback(result);
        }
        return result;
        } else {
        // PVM模式下的原有逻辑
        const params = {
          clusterId: this.clusterId,
        };
        // 等待网络请求结束
        let flag = await this.$axiosPost(global.API.hostCheckCompleted, params);
        // 网络请求结束后才执行下边的语句  如果传入的callback方法为空或者没传内容也不会去执行，这样也不会影响此方法在别处的调用
        if (callback) {
          callback(flag);
        }
        return flag;
      }
    },

    // 表格选择
    onSelectChange(selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys;
    },

    // 表格分页变化
    tableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize;
      this.getEnvironmentList();
    },

    // 计算主机的整体状态（PVM模式）
    calculateHostStatus(host) {
      // 如果主机已经有状态则返回
      if (host.statusStr || host.status) return host.statusStr || host.status;

      // 没有检查项则返回空状态
      const checkItems = host.checkItems || [];
      if (checkItems.length === 0) return null;

      // 如果有检查中的项，则状态为"检查中"
      if (checkItems.some(item => item.status === 'CHECKING')) {
        return 'CHECKING';
      }

      // 如果有等待检查的项，则状态为"等待检查"
      if (checkItems.some(item => item.status === 'WAITING')) {
        return 'WAITING';
      }

      // 如果有失败的项，则状态为"未通过"
      if (checkItems.some(item => item.status === 'FAILED')) {
        return 'FAILED';
      }

      // 如果所有项都是"跳过"，则状态为"已跳过"
      if (checkItems.every(item => item.status === 'SKIPPED')) {
        return 'SKIPPED';
      }

      // 如果有的是跳过有的是成功，则状态为"部分通过"
      if (checkItems.some(item => item.status === 'SKIPPED') &&
          checkItems.some(item => item.status === 'SUCCESS')) {
        return 'MIXED';
      }

      // 默认情况：所有项都通过
      return 'SUCCESS';
    },

    // PVM模式下的其他方法（保持原有逻辑）
    retryEnvironment(row) {
      let hostnames = "";
      if (row === "all") {
        if (this.selectedRowKeys.length < 1) {
          this.$message.warning("请至少选择一台主机！");
          return false;
        }
        hostnames = this.selectedRowKeys.join(",");
      } else {
        hostnames = row.hostname;
      }
      const params = {
        hostnames,
        clusterId: this.clusterId,
        sshUser: this.steps1Data.sshUser,
        sshPort: this.steps1Data.sshPort,
      };
      this.$axiosPost(global.API.rehostCheck, params).then((res) => {
        this.selectedRowKeys = [];
        this.$message.success(`操作成功`);
        this.getEnvironmentList();
      });
    },

    viewLogs(record) {
      // 查看日志的逻辑
      console.log('查看日志:', record);
    },

    // 其他PVM模式的方法...
    handleCheckAction(action) {
      // 处理检查动作
    },

    showHostnameSettingModal() {
      // 显示主机名设置弹窗
    },

    showSyncHostsModal() {
      // 显示同步hosts弹窗
    },

    fixAllFailedItems() {
      // 修复所有失败项
    },

    skipAllFailedItems() {
      // 跳过所有失败项
    },

    closeLogModal() {
      // 关闭日志弹窗
    },

    handleFixCancel() {
      // 处理修复取消
    },

    handleFixConfirm() {
      // 处理修复确认
    },

    cancelHostnameEdit() {
      // 取消主机名编辑
    },

    submitHostnameEdit() {
      // 提交主机名编辑
    },

    closeHostnameSettingModal() {
      // 关闭主机名设置弹窗
    },

    handleHostnameSettingSuccess() {
      // 处理主机名设置成功
    },

    closeSyncHostsModal() {
      // 关闭同步hosts弹窗
    },

    handleSyncHostsSuccess() {
      // 处理同步hosts成功
    }
  }
};
</script>

<style lang="less" scoped>
// 苹果设计系统颜色
@apple-white: #ffffff;
@apple-black: #1d1d1f;
@apple-gray-light: #f5f5f7;
@apple-gray: #86868b;
@apple-blue: #0071e3;
@apple-blue-hover: #147CE5;

// 苹果设计系统字体
.apple-font() {
  font-family: "SF Pro Display", "SF Pro Icons", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}

.steps2 {
  margin: 0;
  max-width: 100%;
  background-color: @apple-white;
  overflow: hidden;
  animation: fadeIn 0.8s ease-out;

  // K8S模式样式
  .k8s-host-check {
  .hero-section {
    text-align: center;
      margin-bottom: 3rem;

    .hero-title {
      .apple-font();
        font-size: 2.4rem;
      font-weight: 600;
      line-height: 1.1;
      letter-spacing: -0.022em;
      color: @apple-black;
      margin-bottom: 0.8rem;
      background: linear-gradient(120deg, @apple-black, #505050);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    .hero-subtitle {
      .apple-font();
      font-size: 1.2rem;
      line-height: 1.4;
      letter-spacing: 0;
      font-weight: 400;
      color: @apple-gray;
        margin: 0;
      max-width: 600px;
        margin: 0 auto;
    }
  }

    .k8s-hosts-container {
      max-width: 1200px;
    margin: 0 auto;
      padding: 0 1rem;
      display: grid;
      grid-template-columns: 1fr 300px;
      gap: 2rem;
      
      .hosts-table-wrapper {
        .k8s-host-table {
          background-color: @apple-white;
          border-radius: 1rem;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
            overflow: hidden;

          /deep/ .ant-table-thead > tr > th {
    background-color: @apple-gray-light;
        border: none;
      .apple-font();
            font-weight: 600;
      color: @apple-black;
    }
          
          /deep/ .ant-table-tbody > tr > td {
        border: none;
            border-bottom: 1px solid @apple-gray-light;
            .apple-font();
  }

  /deep/ .ant-table-tbody > tr:hover > td {
            background-color: rgba(0, 113, 227, 0.02);
          }
          
          .hostname-column {
            .hostname-text {
              .apple-font();
  font-weight: 500;
    color: @apple-black;
              display: block;
}

            .hostname-ip {
              .apple-font();
  font-size: 0.8rem;
              color: @apple-gray;
              margin-top: 0.2rem;
}
}

          .status-indicator {
  display: flex;
  align-items: center;
      .apple-font();
      font-weight: 500;
    }
  }
  }

      .k8s-summary {
        .summary-card {
  background-color: @apple-white;
          border-radius: 1rem;
          padding: 1.5rem;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
          height: fit-content;

          .summary-header {
  display: flex;
  align-items: center;
            margin-bottom: 1.5rem;
            
            .summary-icon {
              color: @apple-blue;
  font-size: 1.2rem;
              margin-right: 0.5rem;
}

            .summary-title {
  .apple-font();
              font-size: 1.1rem;
              font-weight: 600;
  color: @apple-black;
}
}

          .summary-content {
            .summary-item {
  display: flex;
  justify-content: space-between;
    align-items: center;
              margin-bottom: 1rem;
              
              .item-label {
                .apple-font();
                font-size: 0.9rem;
                color: @apple-gray;
  }
  
              .item-value {
                .apple-font();
                font-size: 1rem;
    font-weight: 600;
                
                &.success {
                  color: #52c41a;
  }
  
                &.error {
                  color: #ff4d4f;
  }
              }
            }
          }
        }
      }
    }
  }
  
  // PVM模式样式（保持原有样式）
  .hero-section {
    text-align: center;
    margin-bottom: 2rem;
    
    .hero-title {
      .apple-font();
      font-size: 2.4rem;
      font-weight: 600;
      line-height: 1.1;
      letter-spacing: -0.022em;
      color: @apple-black;
      margin-bottom: 0.8rem;
        }
    
    .hero-subtitle {
  .apple-font();
  font-size: 1.2rem;
      line-height: 1.4;
      letter-spacing: 0;
      font-weight: 400;
      color: @apple-gray;
      margin: 0;
}

    .queue-status-area {
      margin-top: 1rem;
}
}

  .hosts-table-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 1rem;
}
}

// 动画效果
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
}
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
