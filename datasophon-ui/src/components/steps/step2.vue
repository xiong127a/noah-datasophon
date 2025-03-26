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


 * @describe: step2-主机环境校验 
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-08-15 14:07:04
 * @FilePath: \ddh-ui\src\components\steps\step2.vue
-->
<template>
  <div class="steps2 steps">
    <div class="steps-title flex-bewteen-container pdr30">
      <span>主机环境校验</span>
      <div class="queue-controls">
        <queue-status-indicator :queue-status="queueStatus" />
      </div>
    </div>
    <div class="table-info mgt16 steps-body pdr30">
      <a-table 
        @change="tableChange" 
        :columns="columns" 
        :loading="loading" 
        :dataSource="dataSource" 
        :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}" 
        rowKey="hostname" 
        :pagination="pagination"
        expandable
        :expandedRowRender="expandedRowRender"
        :expandIcon="customExpandIcon"
        :class="'host-check-table'"
      >
      </a-table>
    </div>

    <!-- 日志查看弹窗 -->
    <a-modal
      v-model="logVisible"
      :title="logModalTitle"
      width="80%"
      :footer="null"
      @cancel="closeLogModal"
      class="log-modal"
      :bodyStyle="{ padding: '0' }"
    >
      <div class="log-container">
        <div class="log-header">
          <!-- 刷新控制区域 -->
          <div class="header-section">
            <div class="refresh-options">
              <a-button type="primary" @click="refreshLog" :loading="logLoading" class="refresh-btn">
                <a-icon type="reload" />手动刷新
              </a-button>
              <a-dropdown>
                <a-button :type="autoRefreshInterval > 0 ? 'primary' : 'default'" class="auto-refresh-btn">
                  <a-icon :type="autoRefreshInterval > 0 ? 'sync' : 'clock-circle'" :spin="autoRefreshInterval > 0" />
                  <span v-if="autoRefreshInterval === 0">开启自动刷新</span>
                  <span v-else>每 {{autoRefreshInterval}} 秒刷新中</span>
                  <a-icon type="down" style="margin-left: 4px" />
                </a-button>
                <a-menu slot="overlay" @click="handleAutoRefreshChange">
                  <a-menu-item key="0">
                    <a-icon type="stop" />关闭自动刷新
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="1">
                    <a-icon type="sync" />每秒刷新
                  </a-menu-item>
                  <a-menu-item key="3">
                    <a-icon type="sync" />每3秒刷新
                  </a-menu-item>
                  <a-menu-item key="5">
                    <a-icon type="sync" />每5秒刷新
                  </a-menu-item>
                  <a-menu-item key="10">
                    <a-icon type="sync" />每10秒刷新
                  </a-menu-item>
                </a-menu>
              </a-dropdown>
            </div>
          </div>
          
          <!-- 筛选控制区域 -->
          <div class="header-section filter-area">
            <!-- 日志类型选择 -->
            <div class="log-type-selector">
              <div class="filter-title">日志类型筛选：</div>
              <a-radio-group v-model="currentLogType" button-style="solid" @change="handleLogTypeChange">
                <a-radio-button value="all">全部日志</a-radio-button>
                <a-radio-button value="check">检查日志</a-radio-button>
                <a-radio-button value="fix">修复日志</a-radio-button>
              </a-radio-group>
            </div>
            
            <!-- 添加日志筛选组件 -->
            <log-filter 
              ref="logFilter"
              v-if="checkItem && checkItem.clusterId && showLogFilterOptions" 
              :clusterId="checkItem.clusterId" 
              :hostname="checkItem.hostname" 
              :itemId="checkItem.id"
              v-model="logContent"
              hide-reset-button
              @filter-change="handleFilterChange"
            ></log-filter>
          </div>
          
          <!-- 合并的筛选状态显示区域 -->
          <div class="combined-filter-status">
            <div class="filter-description">
              当前显示: 
              <span class="highlight">{{ currentLogType === 'all' ? '全部' : (currentLogType === 'check' ? '检查' : '修复') }}</span> 类型，
              <span v-if="checkItem && $refs.logFilter">
                <template v-if="$refs.logFilter.filterType === 'exact'">
                  <span class="highlight">{{ $refs.logFilter.selectedLevel }}</span> 级别
                </template>
                <template v-else-if="$refs.logFilter.filterType === 'min'">
                  <span class="highlight">{{ $refs.logFilter.selectedLevel }}</span> 及以上级别
                </template>
                <template v-else>
                  <span class="highlight">全部</span> 级别
                </template>
              </span>
              <span v-else>加载中...</span>
              的日志
            </div>
          </div>
        </div>
        <div class="log-content" v-loading="logLoading">
          <pre v-html="logContent"></pre>
        </div>
      </div>
    </a-modal>

    <!-- 确认弹窗 -->
    <a-modal
      v-model="fixConfirmVisible"
      :title="fixConfirmTitle"
      :confirmLoading="fixConfirmLoading"
      @ok="handleFixConfirm"
      @cancel="handleFixCancel"
      okText="确认修复"
      cancelText="取消"
      okType="danger"
      class="fix-confirm-modal"
    >
      <div class="fix-confirm-content">
        <div v-html="fixConfirmContent"></div>
      </div>
    </a-modal>
  </div>
</template>
<script>
import LogFilter from '../log/LogFilter.vue';
import QueueStatusIndicator from '@/components/QueueStatusIndicator'

export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  props: {
    steps1Data: Object,
    depType:String,
  },
  components: {
    LogFilter,
    QueueStatusIndicator
  },
  data() {
    return {
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
      isRequesting: false,
      dataSource: [],
      loading: false,
      firstDataLoaded: false,
      checkItemsMap: {}, // 存储每个主机的校验项
      checkItem: null, // 当前查看日志的检查项
      showLogFilterOptions: true, // 是否显示日志筛选选项
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
      fixConfirmHostname: '',
      fixConfirmItem: null,
      columns: [
        {
          title: "序号",
          key: "index",
          width: 70,
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
        { title: "主机", key: "hostname", dataIndex: "hostname" },
        {
          title: "当前受管",
          key: "managed",
          dataIndex: "managed",
          customRender: (text, row, index) => {
            const h = this.$createElement;
            return h('span', {}, [text ? "是" : "否"]);
          },
        },
        {
          title: "状态",
          key: "hostStatus",
          width: "15%",
          customRender: (text, row) => {
            const h = this.$createElement;

            // 状态映射
            const statusMap = {
              CHECKING: { text: '检查中', color: '#1890ff', icon: 'loading' },
              WAITING: { text: '等待检查', color: '#faad14', icon: 'clock-circle' },
              SUCCESS: { text: '通过', color: '#52c41a', icon: 'check-circle' },
              FAILED: { text: '未通过', color: '#f5222d', icon: 'close-circle' },
              SKIPPED: { text: '已跳过', color: '#d9d9d9', icon: 'stop' },
              TERMINATING: { text: '终止中', color: '#ff7a45', icon: 'stop', spin: true },
              MIXED: { text: '部分通过', color: '#faad14', icon: 'exclamation-circle' }
            };

            // 使用主机的状态
            const hostStatus = row.statusStr || row.status || '';

            // 如果主机有状态，直接显示
            if (hostStatus && statusMap[hostStatus]) {
              const status = statusMap[hostStatus];
              return h('span', {
                class: 'flex-container',
                style: {
                  display: 'flex',
                  alignItems: 'center',
                  color: status.color
                }
              }, [
                h('a-icon', {
                  props: {
                    type: status.icon,
                    theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                    twoToneColor: status.color,
                    spin: status.icon === 'loading'
                  },
                  style: { fontSize: '14px', marginRight: '4px' }
                }),
                h('span', {}, [status.text])
              ]);
            }

            return h('span', {}, ['-']);
          },
        },
        {
          title: "检查项",
          key: "checkItem",
          width: "20%",
          customRender: (text, row) => {
            const h = this.$createElement;

            // 状态映射
            const statusMap = {
              CHECKING: { text: '检查中', color: '#1890ff', icon: 'loading' },
              WAITING: { text: '等待检查', color: '#faad14', icon: 'clock-circle' },
              SUCCESS: { text: '通过', color: '#52c41a', icon: 'check-circle' },
              FAILED: { text: '未通过', color: '#f5222d', icon: 'close-circle' },
              SKIPPED: { text: '已跳过', color: '#d9d9d9', icon: 'stop' },
              TERMINATING: { text: '终止中', color: '#ff7a45', icon: 'stop', spin: true },
              MIXED: { text: '部分通过', color: '#faad14', icon: 'exclamation-circle' }
            };
            
            // 检查主机是否有检查项
            const checkItems = row.checkItems || [];
            
            // 优先级处理：检查中 > 待检查 > 失败 > 跳过 > 成功
            
            // 1. 先检查是否有正在检查中的项目
            const currentItem = checkItems.find(item => item.status === 'CHECKING');
            if (currentItem) {
              const status = statusMap[currentItem.status];
              return h('span', { 
                class: 'flex-container',
                style: { display: 'flex', alignItems: 'center', color: status.color }
              }, [
                h('a-icon', { 
                  props: {
                    type: status.icon,
                    theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                    twoToneColor: status.color,
                    spin: status.icon === 'loading'
                  },
                  style: { fontSize: '14px', marginRight: '4px' }
                }),
                h('span', {}, [currentItem.itemName])
              ]);
            }
            
            // 2. 其次检查是否有待检查的项目
            const waitingItem = checkItems.find(item => item.status === 'WAITING');
            if (waitingItem) {
              const status = statusMap[waitingItem.status];
              return h('span', { 
                class: 'flex-container',
                style: { display: 'flex', alignItems: 'center', color: status.color }
              }, [
                h('a-icon', { 
                  props: {
                    type: status.icon,
                    theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                    twoToneColor: status.color,
                    spin: status.icon === 'loading'
                  },
                  style: { fontSize: '14px', marginRight: '4px' }
                }),
                h('span', {}, [waitingItem.itemName])
              ]);
            }
            
            // 3. 查找失败的项目，并显示最后一个失败项
            const failedItems = checkItems.filter(item => item.status === 'FAILED');
            if (failedItems.length > 0) {
              const lastFailedItem = failedItems[failedItems.length - 1];
              const status = statusMap[lastFailedItem.status];
              return h('span', { 
                class: 'flex-container',
                style: { display: 'flex', alignItems: 'center', color: status.color }
              }, [
                h('a-icon', { 
                  props: {
                    type: status.icon,
                    theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                    twoToneColor: status.color,
                    spin: status.icon === 'loading'
                  },
                  style: { fontSize: '14px', marginRight: '4px' }
                }),
                h('span', {}, [lastFailedItem.itemName])
              ]);
            }
            
            // 4. 查找跳过的项目，显示最后一个跳过项
            const skippedItems = checkItems.filter(item => item.status === 'SKIPPED');
            if (skippedItems.length > 0) {
              const lastSkippedItem = skippedItems[skippedItems.length - 1];
              const status = statusMap[lastSkippedItem.status];
              return h('span', { 
                class: 'flex-container',
                style: { display: 'flex', alignItems: 'center', color: status.color }
              }, [
                h('a-icon', { 
                  props: {
                    type: status.icon,
                    theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                    twoToneColor: status.color,
                    spin: status.icon === 'loading'
                  },
                  style: { fontSize: '14px', marginRight: '4px' }
                }),
                h('span', {}, [lastSkippedItem.itemName])
              ]);
            }
            
            // 5. 最后查找成功的项目，显示最后一个成功项
            const successItems = checkItems.filter(item => item.status === 'SUCCESS');
            if (successItems.length > 0) {
              const lastSuccessItem = successItems[successItems.length - 1];
              const status = statusMap[lastSuccessItem.status];
              return h('span', { 
                class: 'flex-container',
                style: { display: 'flex', alignItems: 'center', color: status.color }
              }, [
                h('a-icon', { 
                  props: {
                    type: status.icon,
                    theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                    twoToneColor: status.color,
                    spin: status.icon === 'loading'
                  },
                  style: { fontSize: '14px', marginRight: '4px' }
                }),
                h('span', {}, [lastSuccessItem.itemName])
              ]);
            }
            
            // 如果没有任何检查项，则显示占位符
            return h('span', {}, ['-']);
          },
        },
        {
          title: "操作",
          key: "action",
          width: "25%",
          customRender: (text, row) => {
            const h = this.$createElement;
            const isChecking = row.status === 'CHECKING' || row.statusStr === 'CHECKING';
            
            return h('div', { class: 'action-buttons' }, [
              // 终止按钮 - 检查中时显示
              isChecking ? h('a-button', {
                attrs: {
                  type: 'danger',
                  size: 'small'
                },
                on: {
                  click: () => this.stopCheck(row)
                }
              }, ["终止"]) : null,
              
              // 重试按钮 - 非检查中时显示，检查中则禁用
              !isChecking ? h('a-button', {
                attrs: {
                  type: 'link',
                  size: 'small',
                  disabled: false // 主机列表的重试按钮始终可用，除非正在检查中
                },
                on: {
                  click: () => this.retryEnvironment(row)
                }
              }, ["重试"]) : null
            ].filter(Boolean));
          },
        },
      ],
      selectedCheckItems: {}, // 存储每个主机选中的检查项 { hostname: [itemName1, itemName2] }
      logVisible: false,
      logModalTitle: '',
      logContent: '',
      logLoading: false,
      autoRefreshInterval: 0,
      currentLogHostname: null,
      currentLogItemId: null,
      currentLogItemName: null,
      refreshTimer: null,
      currentLogType: 'all',
      forceUseTypedApi: false,
    };
  },
  computed: {
    // 计算是否有任何主机的检查项正在检查中
    hasAnyCheckingItems() {
      if (!this.dataSource || this.dataSource.length === 0) {
        return false;
      }
      
      return this.dataSource.some(host => {
        const checkItems = this.checkItemsMap[host.hostname] || [];
        return checkItems.some(item => item.status === 'CHECKING');
      });
    }
  },
  methods: {
    tableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize
      this.pollingSearch();
    },
    getEnvironmentList(flag) {
      // 只在请求进行中时跳过新的请求
      if (this.isRequesting) {
        console.log('上一次请求还在进行中，跳过本次请求');
        return;
      }
      
      if (!flag) this.loading = true;
      const params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
        clusterId: this.clusterId,
        ...this.steps1Data,
      };
      
      this.isRequesting = true;
      
      this.$axiosPost(global.API.analysisHostList, params)
        .then((res) => {
          if (res.code === 200) {
            // 处理主机状态
            if (res.data && res.data.length > 0) {
              res.data.forEach(host => {
                // 计算主机状态
                host.status = this.calculateHostStatus(host);
                
                // 将返回的checkItems数据保存到checkItemsMap中
                if (host.checkItems) {
                  this.$set(this.checkItemsMap, host.hostname, host.checkItems);
                }
              });
              
              // 只在第一次成功获取数据时调用批量检查
              if (!this.firstDataLoaded) {
                this.startBatchCheckHosts(res.data);
                this.firstDataLoaded = true; // 标记已经加载过数据
              }
            }
            
            this.dataSource = res.data;
            this.pagination.total = res.total;
            
            // 保存队列状态信息
            if (res.queueStatus) {
              this.queueStatus = res.queueStatus;
            }

            if (this.depType=='K8S'){
              let data = JSON.parse(JSON.stringify(res.data))
              data && data.forEach(e => {
                if (e.checkResult.code=='10001'){
                  e['CheckResult'] = e.checkResult
                  delete e.checkResult
                  let arr=[]
                  arr[0] = e
                  this.saveK8sHostApi(arr)
                } 
              })
            }
          }
        })
        .catch((error) => {
          console.error('请求失败:', error);
        })
        .finally(() => {
          // 无论成功还是失败，都要重置状态
          this.loading = false;
          this.isRequesting = false;
        });
    },
    
    /**
     * 启动批量检查主机
     * 提取主机名列表，调用后端接口开始检查
     */
    async startBatchCheckHosts(hosts) {
      if (!hosts || hosts.length === 0) {
        return;
      }
      
      // 提取需要检查的主机名列表
      // 只对未受管或状态为WAITING的主机进行检查
      const hostnamesToCheck = hosts
        .filter(host => !host.managed || host.status === 'WAITING')
        .map(host => host.hostname);
      
      if (hostnamesToCheck.length === 0) {
        console.log('没有需要检查的主机');
        return;
      }
      
      // 调用批量检查API
      try {
        const res = await this.$axiosJsonPost(global.API.batchCheckHosts + '?clusterId=' + this.clusterId, hostnamesToCheck);
        if (res.code === 200) {
          console.log('成功启动主机检查:', res.msg);
          
          // 立即刷新一次，不等待5秒后的自动刷新
          this.getEnvironmentList(false);
        } else {
          console.warn('启动主机检查失败:', res.msg);
        }
      } catch (err) {
        console.error('调用批量检查API失败:', err);
      }
    },
    
    // 计算主机的整体状态
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
    saveK8sHostApi (params){
      this.$axiosJsonPost(global.API.saveK8sHost + '?clusterId=' + this.clusterId, params).then((res) => {
       });
    },
    //表格选择
    onSelectChange(selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys;
    },
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
        this.pollingSearch();
      });
    },
    // 主机环境校验是否完成 是否可以进入下一步
    async hostCheckCompleted(callback) {
      const params = {
        clusterId: this.clusterId,
      };
      // 等待网络请求结束
      let flag = await this.$axiosPost(global.API.hostCheckCompleted, params);
      // 网络请求结束后才执行下边的语句  如果传入的callback方法为空或者没传内容也不会去执行，这样也不会影响此方法在别处的调用
      if (callback) {
        callback(flag);
      }
    },
    // 展开行渲染函数
    expandedRowRender(record) {
      const h = this.$createElement;
      const checkItems = this.checkItemsMap[record.hostname] || [];
      
      // 判断主机是否处于检查中状态
      const isHostChecking = record.status === 'CHECKING' || record.statusStr === 'CHECKING';
      
      const columns = [
        {
          title: '检查项',
          dataIndex: 'itemName',
          key: 'itemName',
          width: '25%'
        },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: '15%',
          customRender: (text) => {
            const h = this.$createElement;
            // 使用字符串类型的状态映射
            const statusMap = {
              'WAITING': { text: '待检查', color: '#faad14', icon: 'clock-circle' },
              'SUCCESS': { text: '通过', color: '#52c41a', icon: 'check-circle' },
              'FAILED': { text: '未通过', color: '#f5222d', icon: 'close-circle' },
              'CHECKING': { text: '检查中', color: '#1890ff', icon: 'loading' },
              'SKIPPED': { text: '已跳过', color: '#d9d9d9', icon: 'warning' },
              'TERMINATING': { text: '终止中', color: '#ff7a45', icon: 'stop', spin: true },
              'FIXING': { text: '修复中', color: '#1890ff', icon: 'tool', spin: true }
            };

            const status = statusMap[text] || { text: '未知', color: '#999999', icon: 'question-circle' };

            return h('span', { style: { color: status.color } }, [
              h('a-icon', {
                props: {
                  type: status.icon,
                  theme: !['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined,
                  twoToneColor: status.color,
                  spin: status.icon === 'loading'
                },
                style: { marginRight: '4px' }
              }),
              status.text
            ]);
          }
        },
        {
          title: '检查结果',
          dataIndex: 'message',
          key: 'message',
          width: '20%',
          customRender: (text, row) => {
            const h = this.$createElement;
            
            // 检查文本是否存在
            if (!text) return h('span', {}, ['-']);
            
            // 不再限制文本长度，所有检查项都显示悬浮提示
            const displayText = text;
            
            // 使用a-tooltip组件提供鼠标悬浮显示完整内容的功能
            return h('a-tooltip', {
              props: {
                title: h('div', { 
                  class: 'check-result-tooltip',
                  style: {
                    maxWidth: '1200px',
                    padding: '16px',
                    borderRadius: '8px',
                    boxShadow: '0 6px 16px rgba(0, 0, 0, 0.2)',
                    background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)',
                    color: '#333',
                    fontSize: '14px',
                    lineHeight: '1.6',
                    wordBreak: 'break-word',
                    whiteSpace: 'pre-wrap'
                  }
                }, [
                  // 添加标题行
                  h('div', {
                    style: {
                      fontWeight: 'bold',
                      marginBottom: '6px',
                      borderBottom: '1px solid rgba(0, 0, 0, 0.1)',
                      paddingBottom: '6px',
                      color: row.status === 'SUCCESS' ? '#52c41a' : 
                             row.status === 'FAILED' ? '#f5222d' :
                             row.status === 'SKIPPED' ? '#d9d9d9' :
                             row.status === 'CHECKING' ? '#1890ff' : 
                             row.status === 'FIXING' ? '#1890ff' : '#333'
                    }
                  }, [
                    h('a-icon', {
                      props: {
                        type: row.status === 'SUCCESS' ? 'check-circle' :
                              row.status === 'FAILED' ? 'close-circle' :
                              row.status === 'SKIPPED' ? 'warning' :
                              row.status === 'CHECKING' ? 'loading' :
                              row.status === 'FIXING' ? 'tool' : 'info-circle',
                        theme: row.status !== 'CHECKING' && row.status !== 'FIXING' ? 'filled' : undefined,
                        spin: row.status === 'CHECKING' || row.status === 'FIXING'
                      },
                      style: {
                        marginRight: '8px',
                      }
                    }),
                    '检查结果详情'
                  ]),
                  // 添加检查结果内容
                  h('div', {
                    domProps: {
                      innerHTML: text
                    }
                  })
                ]),
                placement: 'top',
                mouseEnterDelay: 0.3,
                overlayClassName: 'custom-tooltip-overlay',
                autoAdjustOverflow: true,
                arrowPointAtCenter: true,
                align: {
                  offset: [0, 0]
                }
              }
            }, [
              h('span', {
                style: {
                  cursor: 'pointer',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                  display: 'inline-block',
                  maxWidth: '100%',
                  borderBottom: '1px dotted #ccc',
                  color: row.status === 'SUCCESS' ? '#52c41a' : 
                         row.status === 'FAILED' ? '#f5222d' :
                         row.status === 'CHECKING' ? '#1890ff' :
                         row.status === 'SKIPPED' ? '#d9d9d9' :
                         row.status === 'FIXING' ? '#1890ff' : '#333',
                  transition: 'color 0.3s'
                },
                class: 'check-result-text'
              }, [
                // 在这里使用一个辅助函数去除HTML标签，只显示纯文本
                h('span', {}, [
                  displayText.length > 20 ? 
                    this.stripHtml(displayText).substr(0, 20) + '...' : 
                    this.stripHtml(displayText)
                ])
              ])
            ]);
          }
        },
        {
          title: '操作',
          key: 'action',
          width: '25%',
          customRender: (text, row) => {
            const h = this.$createElement;
            const isChecking = row.status === 'CHECKING';
            const isFailed = row.status === 'FAILED';
            
            return h('div', { class: 'action-buttons' }, [
              // 终止按钮 - 检查中时显示
              isChecking ? h('a-button', {
                attrs: {
                  type: 'danger',
                  size: 'small'
                },
                on: {
                  click: () => this.stopCheckItem(record.hostname, row.id)
                }
              }, ["终止"]) : null,
              
              // 重试按钮 - 非检查中时显示，检查中则禁用
              !isChecking ? h('a-button', {
                attrs: {
                  type: 'link',
                  size: 'small',
                  // 修改：处于CHECKING或FIXING状态时禁用重试按钮
                  disabled: row.status === 'CHECKING' || row.status === 'FIXING' || 
                           !((row.status === 'FAILED' || row.status === 'SUCCESS' || row.status === 'SKIPPED'))
                },
                on: {
                  click: () => this.retryCheckItem(row.hostname, row.id)
                }
              }, ["重试"]) : null,
              
              // 修复按钮 - 失败时可用，主机整体检查中时禁用
              isFailed ? h('a-button', {
                attrs: {
                  type: 'link',
                  size: 'small',
                  // 当项目正在修复中时也禁用修复按钮
                  disabled: isHostChecking || row.status === 'FIXING'
                },
                on: {
                  click: () => this.fixCheckItem(record.hostname, row)
                }
              }, ["修复"]) : null,
              
              // 跳过按钮 - 失败时可用，主机整体检查中时禁用
              isFailed ? h('a-button', {
                attrs: {
                  type: 'link',
                  size: 'small',
                  // 当主机整体状态为检查中时，禁用按钮
                  disabled: isHostChecking
                },
                on: {
                  click: () => this.skipCheckItem(record.hostname, row.id)
                }
              }, ["跳过"]) : null
            ].filter(Boolean));
          }
        },
        {
          title: '日志',
          key: 'log',
          width: '15%',
          customRender: (text, row) => {
            const h = this.$createElement;
            return h('a-button', {
              attrs: {
                type: 'link',
                size: 'small',
                disabled: row.status === 'WAITING'
              },
              on: {
                click: () => this.viewItemLog(record.hostname, row.id, row.itemName)
              }
            }, ["查看日志"]);
          }
        }
      ];

      // 创建header-summary部分
      const headerSummary = h('div', { class: 'header-summary' }, [
        h('span', {}, [`共 ${checkItems.length} 项检查`]),
        h('a-divider', { props: { type: 'vertical' } }),
        h('span', { style: { color: '#52c41a' } }, [
          h('a-icon', { 
            props: { 
              type: 'check-circle', 
              theme: 'twoTone', 
              twoToneColor: '#52c41a' 
            }, 
            style: { marginRight: '4px' } 
          }),
          `${checkItems.filter(item => item.status === 'SUCCESS').length} 项通过`
        ]),
        h('a-divider', { props: { type: 'vertical' } }),
        h('span', { style: { color: '#f5222d' } }, [
          h('a-icon', { 
            props: { 
              type: 'close-circle', 
              theme: 'twoTone', 
              twoToneColor: '#f5222d' 
            }, 
            style: { marginRight: '4px' } 
          }),
          `${checkItems.filter(item => item.status === 'FAILED').length} 项失败`
        ]),
        h('a-divider', { props: { type: 'vertical' } }),
        h('span', { style: { color: '#faad14' } }, [
          h('a-icon', { 
            props: { type: 'clock-circle' }, 
            style: { marginRight: '4px' } 
          }),
          `${checkItems.filter(item => item.status === 'WAITING').length} 项待检查`
        ]),
        h('a-divider', { props: { type: 'vertical' } }),
        h('span', { style: { color: '#1890ff' } }, [
          h('a-icon', { 
            props: { 
              type: 'loading',
              spin: true
            }, 
            style: { marginRight: '4px' } 
          }),
          `${checkItems.filter(item => item.status === 'CHECKING').length} 项检查中`
        ]),
        h('a-divider', { props: { type: 'vertical' } }),
        h('span', { style: { color: '#d9d9d9' } }, [
          h('a-icon', { 
            props: { 
              type: 'warning', 
              theme: 'twoTone', 
              twoToneColor: '#d9d9d9' 
            }, 
            style: { marginRight: '4px' } 
          }),
          `${checkItems.filter(item => item.status === 'SKIPPED').length} 项已跳过`
        ])
      ]);

      // 创建header-actions部分
      const headerActions = h('div', { class: 'header-actions' }, [
        h('a-button', {
          attrs: {
            type: 'primary',
            size: 'small',
            // 修改：不再基于整个主机的检查状态禁用按钮
            // 只检查是否有可重试的选中项
            disabled: !this.hasRetryableSelectedItems(record.hostname)
          },
          style: { marginRight: '8px' },
          on: {
            click: () => this.retrySelectedItems(record.hostname)
          }
        }, ['重试选中项']),
        h('a-button', {
          attrs: {
            type: 'primary',
            size: 'small',
            // 修改：不再基于整个主机的检查状态禁用按钮
            // 只检查是否有可修复的选中项
            disabled: !this.hasFixableSelectedItems(record.hostname)
          },
          on: {
            click: () => this.fixSelectedItems(record.hostname)
          }
        }, ['修复选中项'])
      ]);

      // 创建表格
      return h('div', { class: 'check-items-container' }, [
        h('div', { class: 'check-items-header' }, [
          headerSummary,
          headerActions
        ]),
        h('a-table', {
          props: {
            columns: columns,
            dataSource: checkItems,
            pagination: false,
            size: 'middle',
            rowKey: 'id',
            rowSelection: {
              selectedRowKeys: this.selectedCheckItems[record.hostname] || [],
              onChange: (selectedRowKeys) => this.onCheckItemSelect(record.hostname, selectedRowKeys)
            }
          }
        })
      ]);
    },

    // 选择检查项
    onCheckItemSelect(hostname, selectedRowKeys) {
      this.$set(this.selectedCheckItems, hostname, selectedRowKeys);
    },

    // 重试选中的检查项
    async retrySelectedItems(hostname) {
      const selectedItems = this.selectedCheckItems[hostname] || [];
      if (selectedItems.length === 0) {
        this.$message.warning('请选择要重试的检查项');
        return;
      }

      try {
        const res = await this.$axiosPost(global.API.retryCheckItems, {
          clusterId: this.clusterId,
          hostname,
          itemNames: selectedItems
        });
        if (res.code === 200) {
          this.$message.success('重试指令已发送');
          // 清空选择
          this.$set(this.selectedCheckItems, hostname, []);
          
          // 立即刷新一次，不等待5秒后的自动刷新
          this.getEnvironmentList(false);
        }
      } catch (error) {
        console.error('重试检查项失败:', error);
        this.$message.error('重试检查项失败');
      }
    },

    // 跳过检查项
    async skipCheckItem(hostname, itemId) {
      // 查找对应的主机信息
      const host = this.dataSource.find(h => h.hostname === hostname);
      
      // 检查主机是否处于检查中状态
      if (host && (host.status === 'CHECKING' || host.statusStr === 'CHECKING')) {
        this.$message.warning('主机当前正在检查中，请稍后再尝试跳过');
        return;
      }
      
      try {
        const res = await this.$axiosPost(global.API.skipCheckItem, {
          clusterId: this.clusterId,
          hostname,
          itemId
        });
        if (res.code === 200) {
          this.$message.success('已跳过该检查项');
          
          // 更新检查项状态
          const items = this.checkItemsMap[hostname];
          if (items) {
            const targetItem = items.find(item => item.id === itemId);
            if (targetItem) {
              targetItem.status = 'SKIPPED';
              targetItem.message = '已手动跳过此检查项';
              
              // 使用...items创建新数组，确保Vue能检测到变化
              this.$set(this.checkItemsMap, hostname, [...items]);
              
              // 延迟1秒后刷新列表获取服务器最新状态
              setTimeout(() => {
                this.refreshHostList();
              }, 1000);
            }
          }
        }
      } catch (error) {
        console.error('跳过检查项失败:', error);
        this.$message.error('跳过检查项失败');
      }
    },

    // 获取主机校验项
    async getHostCheckItems(hostname, isFirstHost = false) {
      try {
        const res = await this.$axiosGet(global.API.getHostCheckItems + '?hostname=' + hostname + '&clusterId=' + this.clusterId);
        if (res.code === 200) {
          // 设置检查项数据
          this.$set(this.checkItemsMap, hostname, res.data);
        }
      } catch (error) {
        console.error('获取主机校验项失败:', error);
        this.$message.error('获取主机校验项失败');
      }
    },

    // 使用自定义a-modal组件实现确认弹窗
    async fixCheckItem(hostname, item) {
      if (item.status !== 'FAILED') return;
      
      // 查找对应的主机信息
      const host = this.dataSource.find(h => h.hostname === hostname);
      
      // 删除以下检查，允许在主机检查过程中也能点击修复按钮
      /* 
      if (host && (host.status === 'CHECKING' || host.statusStr === 'CHECKING')) {
        this.$message.warning('主机当前正在检查中，请稍后再尝试修复');
        return;
      }
      */
      
      try {
        // 获取确认信息
        const res = await this.$axiosGet(global.API.getCheckItemConfirmInfo, {
          clusterId: this.clusterId,
          hostname: hostname,
          itemId: item.id
        });
        
        console.log('获取确认信息结果:', res); // 添加调试日志
        
        if (res.code === 200) {
          const needConfirm = res.needConfirm;
          const confirmMessage = res.confirmMessage;
          const itemName = res.itemName || item.itemName;
          
          if (needConfirm) {
            // 设置弹窗数据
            this.fixConfirmTitle = '确认修复 - ' + itemName;
            this.fixConfirmContent = confirmMessage;
            this.fixConfirmHostname = hostname;
            this.fixConfirmItem = item;
            
            // 显示弹窗
            this.fixConfirmVisible = true;
          } else {
            // 无需确认，直接修复
            this.doFixCheckItem(hostname, item, false);
          }
        } else {
          // 处理API错误
          this.$message.error(res.msg || '获取确认信息失败');
        }
      } catch (err) {
        console.error('获取确认信息失败:', err);
        this.$message.error('获取确认信息失败');
      }
    },

    // 处理确认修复对话框的确认按钮
    handleFixConfirm() {
      this.fixConfirmLoading = true;
      
      // 执行修复
      this.doFixCheckItem(this.fixConfirmHostname, this.fixConfirmItem, true).then(() => {
        this.fixConfirmVisible = false;
        this.fixConfirmLoading = false;
      }).catch(() => {
        this.fixConfirmLoading = false;
      });
    },

    // 处理取消对话框
    handleFixCancel() {
      this.fixConfirmVisible = false;
    },

    // 执行实际的修复操作
    async doFixCheckItem(hostname, item, skipConfirm) {
      console.log('执行修复操作:', hostname, item.id, skipConfirm); // 添加调试日志
      
      // Set loading state
      this.$set(item, 'fixing', true);
      
      try {
        // Call fix API
        const res = await this.$axiosPost(global.API.fixCheckItem, {
          clusterId: this.clusterId,
          hostname: hostname,
          itemId: item.id,
          skipConfirm: skipConfirm
        });
        
        if (res.code === 200) {
          this.$message.success('修复操作已提交');
          // Refresh check item status after a delay
          setTimeout(() => {
            this.getHostCheckItems(hostname);
          }, 1000);
        }
        
        // 清理状态
        this.$set(item, 'fixing', false);
        return res;
      } catch (err) {
        console.error('修复失败:', err);
        // 只在网络错误等前端异常情况下显示通用错误消息
        this.$message.error('请求失败，请检查网络连接');
        this.$set(item, 'fixing', false);
        throw err;
      }
    },

    // 修复所有检查项
    async fixAllCheckItems(hostname) {
      try {
        const res = await this.$axiosPost(global.API.fixAllCheckItems, {
          clusterId: this.clusterId,
          hostname
        });
        if (res.code === 200) {
          this.$message.success('修复指令已发送');
          // 通过轮询获取最新状态
          this.pollingSearch();
        }
      } catch (error) {
        console.error('修复所有检查项失败:', error);
        this.$message.error('修复所有检查项失败');
      }
    },

    // 添加重试单个检查项的方法
    async retryCheckItem(hostname, itemId) {
      // 查找对应的主机信息
      const host = this.dataSource.find(h => h.hostname === hostname);
      
      // 删除以下检查，允许在主机检查过程中也能点击重试按钮
      /*
      if (host && (host.status === 'CHECKING' || host.statusStr === 'CHECKING')) {
        this.$message.warning('主机当前正在检查中，请稍后再尝试重试');
        return;
      }
      */
      
      try {
        const res = await this.$axiosPost(global.API.retryCheckItems, {
          clusterId: this.clusterId,
          hostname,
          itemNames: [itemId]  // 只重试选中的单个检查项
        });
        
        if (res.code === 200) {
          this.$message.success('重试指令已发送');
          
          // 立即刷新一次，不等待5秒后的自动刷新
          this.getEnvironmentList(false);
        } else {
          this.$message.error(res.msg || '重试检查项失败');
        }
      } catch (error) {
        console.error('重试检查项失败:', error);
        this.$message.error('重试检查项失败');
      }
    },

    // 自定义展开图标
    customExpandIcon({ expanded, onExpand, record }) {
      const h = this.$createElement;
      return h('div', {
        class: 'expand-icon-wrapper',
        on: {
          click: (e) => {
            onExpand(record, e);
          }
        }
      }, [
        h('a-icon', {
          class: 'expand-icon',
          props: {
            type: expanded ? 'down' : 'right'
          }
        }),
        h('span', { class: 'expand-text' }, [
          expanded ? '收起详情' : '查看详情'
        ])
      ]);
    },

    // 检查是否有可修复的选中项
    hasFixableSelectedItems(hostname) {
      const selectedItems = this.selectedCheckItems[hostname] || [];
      const items = this.checkItemsMap[hostname] || [];
      return selectedItems.some(itemId => {
        const item = items.find(i => i.id === itemId);
        return item && item.status === 'FAILED';
      });
    },

    // 修复选中的检查项
    async fixSelectedItems(hostname) {
      const selectedItems = this.selectedCheckItems[hostname] || [];
      if (selectedItems.length === 0) {
        this.$message.warning('请选择要修复的检查项');
        return;
      }

      try {
        const res = await this.$axiosPost(global.API.fixSelectedCheckItems, {
          clusterId: this.clusterId,
          hostname,
          itemIds: selectedItems.join(',')
        });
        
        if (res.code === 200) {
          this.$message.success('修复指令已发送');
          // 清空选择
          this.$set(this.selectedCheckItems, hostname, []);
          // 通过轮询获取最新状态
          this.pollingSearch();
        }
      } catch (error) {
        // 异常情况的日志记录，但不显示错误弹窗
        console.error('修复选中检查项失败:', error);
      }
    },

    // 检查是否有可重试的选中项
    hasRetryableSelectedItems(hostname) {
      const selectedItems = this.selectedCheckItems[hostname] || [];
      const items = this.checkItemsMap[hostname] || [];
      
      if (selectedItems.length === 0) {
        return false;
      }
      
      return selectedItems.some(itemId => {
        const item = items.find(i => i.id === itemId);
        // 允许重试所有非CHECKING和非TERMINATING状态的检查项
        return item && item.status !== 'CHECKING' && item.status !== 'TERMINATING'
          && (item.status === 'FAILED' || item.status === 'SUCCESS' || item.status === 'SKIPPED');
      });
    },

    /**
     * 停止主机检查
     */
    stopCheck(row) {
      if (!row || !row.hostname) {
        this.$message.error('参数错误：主机名不能为空');
        return;
      }

      try {
        // 立即将该主机所有正在检查的项状态更新为"终止中"
        const items = this.checkItemsMap[row.hostname] || [];
        let hasUpdated = false;
        
        if (items && items.length > 0) {
          items.forEach(item => {
            if (item.status === 'CHECKING') {
              item.status = 'TERMINATING';
              item.message = '正在终止检查...';
              hasUpdated = true;
            }
          });
          
          if (hasUpdated) {
            // 更新本地缓存，确保UI立即显示变化
            this.$set(this.checkItemsMap, row.hostname, [...items]);
          }
        }

        // 调用后端API
        this.$axiosPost(global.API.stopHostCheck, { 
          clusterId: this.clusterId,
          hostname: row.hostname
        }).then(res => {
          if (res && res.code === 200) {
            this.$message.success('已终止主机检查');
            
            // 延迟1秒后刷新列表，获取最新状态
            setTimeout(() => {
              this.refreshHostList();
            }, 1000);
          } else {
            this.$message.error(res.msg || '终止检查失败');
            this.refreshHostList(); // 还是需要刷新，恢复状态
          }
        }).catch(error => {
          console.error('终止主机检查失败:', error);
          this.$message.error('终止主机检查失败，请检查网络连接');
          this.refreshHostList(); // 出错时也刷新，恢复状态
        });
      } catch (error) {
        console.error('终止主机检查异常:', error);
        this.$message.error('终止主机检查出现异常');
        this.refreshHostList();
      }
    },
    
    /**
     * 重试主机检查
     */
    retryCheck(row) {
      this.$axiosPost(global.API.rehostCheck, { 
        clusterId: this.clusterId,
        hostnames: row.hostname
      }).then(res => {
        if (res.code === 200) {
          this.$message.success('已重新开始检查');
          this.refreshHostList();
        } else {
          this.$message.error(res.msg || '重新检查失败');
        }
      });
    },
    
    /**
     * 刷新主机列表
     */
    refreshHostList() {
      // 刷新主机列表数据
      this.getEnvironmentList();
    },

    /** 
     * 检查所有主机
     */
    checkAllHosts() {
    },

    // 五秒去刷一下
    pollingSearch() {
      this.getEnvironmentList(); // 先立马刷一次
      let self = this;
      if (self.timer) clearInterval(self.timer);
      self.timer = setInterval(() => {
        self.getEnvironmentList(true);
      }, 5000);
    },

    // 查看日志
    viewItemLog(hostname, itemId, itemName) {
      // 保存当前选择的检查项信息
      this.currentLogHostname = hostname;
      this.currentLogItemId = itemId;
      this.currentLogItemName = itemName;
      
      // 设置当前检查项信息，用于日志筛选组件
      this.checkItem = {
        clusterId: this.clusterId,
        hostname: hostname,
        id: itemId,
        itemName: itemName
      };
      
      // 打开日志弹窗并加载日志
      this.logModalTitle = `日志 - 主机: ${hostname}, 检查项: ${itemName}`;
      this.logVisible = true;
      this.logContent = '';
      
      // 设置初始日志类型为全部日志
      this.currentLogType = 'all';
      
      // 初始停止之前可能存在的自动刷新定时器
      this.stopAutoRefresh();

      // 等待DOM更新完成后设置初始筛选条件
      this.$nextTick(() => {
        if (this.$refs.logFilter) {
          // 设置默认筛选条件
          this.$refs.logFilter.filterType = 'min';
          this.$refs.logFilter.selectedLevel = 'INFO';
          // 手动触发筛选
          this.$refs.logFilter.applyFilter();
        }
        // 获取日志数据
        this.fetchItemLog();
      });
    },
    
    // 获取检查项日志
    async fetchItemLog() {
      if (!this.currentLogHostname || !this.currentLogItemId) {
        return;
      }
      
      this.logLoading = true;
      
      try {
        // 统一使用一个API进行所有筛选
        const apiUrl = '/ddh/host/check/getLog';
        
        // 获取级别筛选参数
        let logLevel = 'INFO'; // 默认显示INFO级别
        let filterMode = 'min'; // 默认显示INFO及以上级别
        
        if (this.$refs.logFilter) {
          filterMode = this.$refs.logFilter.filterType;
          logLevel = this.$refs.logFilter.selectedLevel;
        }
          
        // 准备请求参数
        const params = { 
          clusterId: this.clusterId,
          hostname: this.currentLogHostname,
          itemId: this.currentLogItemId,
          logType: this.currentLogType,
          logLevel: logLevel,
          filterMode: filterMode
        };
        
        console.log('API请求:', apiUrl, params);
        const res = await this.$axiosPost(apiUrl, params);
        
        if (res.code === 200) {
          this.logContent = res.data || '暂无日志数据';
        } else {
          this.logContent = `获取日志失败: ${res.msg || '未知错误'}`;
          if (this.autoRefreshInterval > 0) {
            this.stopAutoRefresh(); // 如果获取失败，停止自动刷新
            this.$message.error('日志获取失败，已停止自动刷新');
          }
        }
      } catch (error) {
        console.error('获取日志失败:', error);
        this.logContent = '获取日志失败，请稍后重试';
        if (this.autoRefreshInterval > 0) {
          this.stopAutoRefresh(); // 如果获取失败，停止自动刷新
          this.$message.error('日志获取失败，已停止自动刷新');
        }
      } finally {
        this.logLoading = false;
      }
    },
    
    // 手动刷新日志
    refreshLog() {
      this.fetchItemLog();
    },
    
    // 处理自动刷新间隔变化
    handleAutoRefreshChange(e) {
      const value = parseInt(e.key);
      
      // 停止之前的自动刷新
      this.stopAutoRefresh();
      
      // 设置新的自动刷新间隔
      this.autoRefreshInterval = value;
      
      // 如果选择了自动刷新，启动定时器
      if (value > 0) {
        this.$message.success(`已开启自动刷新(${value}秒)`);
        this.startAutoRefresh();
      } else {
        this.$message.info('已关闭自动刷新');
      }
    },
    
    // 启动自动刷新
    startAutoRefresh() {
      if (this.autoRefreshInterval > 0) {
        this.refreshTimer = setInterval(() => {
          this.fetchItemLog();
        }, this.autoRefreshInterval * 1000);
      }
    },
    
    // 停止自动刷新
    stopAutoRefresh() {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer);
        this.refreshTimer = null;
      }
    },
    
    // 重置日志筛选，显示INFO级以上的全部类型日志
    resetLogFilter() {
      this.currentLogType = 'all';
      if (this.$refs.logFilter) {
        this.$refs.logFilter.filterType = 'min';
        this.$refs.logFilter.selectedLevel = 'INFO';
        this.$refs.logFilter.applyFilter();
      }
      this.fetchItemLog();
    },

    // 终止单个检查项
    async stopCheckItem(hostname, itemId) {
      if (!hostname || !itemId) {
        this.$message.error('参数错误：主机名或检查项ID不能为空');
        return;
      }

      try {
        // 立即将检查项状态更新为"终止中"，提供用户视觉反馈
        const items = this.checkItemsMap[hostname] || [];
        const targetItem = items.find(item => item.id === itemId);
        
        if (targetItem && targetItem.status === 'CHECKING') {
          targetItem.status = 'TERMINATING';
          targetItem.message = '正在终止检查...';
          // 更新本地缓存，确保UI立即显示变化
          this.$set(this.checkItemsMap, hostname, [...items]);
        }

        // 调用后端API
        const res = await this.$axiosPost(global.API.stopCheckItem, { 
          clusterId: this.clusterId,
          hostname: hostname,
          itemId: itemId
        });
        
        if (res && res.code === 200) {
          this.$message.success('已终止检查项');
          
          // 延迟1秒后刷新列表，获取最新状态
          setTimeout(() => {
            this.refreshHostList();
          }, 1000);
        } else {
          this.$message.error(res.msg || '终止检查失败');
          this.refreshHostList(); // 还是需要刷新，恢复状态
        }
      } catch (error) {
        console.error('终止检查项失败:', error);
        this.$message.error('终止检查项失败，请检查网络连接');
        this.refreshHostList(); // 出错时也刷新，恢复状态
      }
    },

    // 关闭日志查看弹窗
    closeLogModal() {
      this.logVisible = false;
      this.stopAutoRefresh();
      this.currentLogHostname = null;
      this.currentLogItemId = null;
      this.currentLogItemName = null;
      // 清理checkItem
      this.checkItem = null;
    },

    handleLogTypeChange() {
      // 当日志类型变化时，重新获取日志
      this.fetchItemLog();
      
      // 随着系统扩展，如果添加了新的日志类型，在这里不需要特殊处理
      // 只需要:
      // 1. 在上面的日志类型选择器中添加新的a-radio-button
      // 2. 确保后端OperationType枚举中添加了相应的类型
      // 3. 确保HostCheckServiceImpl.getCheckItemLogWithType方法支持新的日志类型
    },

    // 处理筛选变化
    handleFilterChange(filterData) {
      // 直接刷新日志以应用新的筛选条件
      this.fetchItemLog();
    },
    
    // 去除HTML标签的辅助函数
    stripHtml(html) {
      if (!html) return '';
      const tmp = document.createElement('DIV');
      tmp.innerHTML = html;
      return tmp.textContent || tmp.innerText || '';
    },

    // 找到渲染检查项状态的方法或函数，添加对FIXING状态的处理
    getStatusText(status) {
      if (!status) return '未知';
      
      const statusMap = {
        'SUCCESS': '成功',
        'FAILED': '失败',
        'CHECKING': '检查中',
        'FIXING': '修复中', // 添加对FIXING状态的处理
        'WAITING': '等待检查'
      };
      
      return statusMap[status] || '未知';
    },

    // 找到设置状态样式的方法，添加FIXING状态的样式
    getStatusStyle(status) {
      if (!status) return '';
      
      const styleMap = {
        'SUCCESS': 'success-status',
        'FAILED': 'failed-status',
        'CHECKING': 'checking-status',
        'FIXING': 'fixing-status', // 添加FIXING状态的样式
        'WAITING': 'waiting-status'
      };
      
      return styleMap[status] || '';
    },
  },
  mounted() {
    // 直接开始轮询
    this.pollingSearch();
    
    // 注册新的统一日志API
    if (!global.API.getLog) {
      global.API.getLog = '/ddh/host/check/getLog';
    }

    // 添加复制功能到window对象
    window.copyToClipboard = (text) => {
      navigator.clipboard.writeText(text).then(() => {
        this.$message.success('已复制到剪贴板');
      }).catch(() => {
        // 如果clipboard API失败，使用传统方法
        const textarea = document.createElement('textarea');
        textarea.value = text;
        document.body.appendChild(textarea);
        textarea.select();
        try {
          document.execCommand('copy');
          this.$message.success('已复制到剪贴板');
        } catch (err) {
          this.$message.error('复制失败');
        }
        document.body.removeChild(textarea);
      });
    };
  },
  beforeDestroy() {
    // 清理定时器
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
    
    // 清理日志刷新定时器
    this.stopAutoRefresh();

    // 清理window对象上的方法
    window.copyToClipboard = undefined;
  },
};
</script>
<style lang="less" scoped>
.check-items-container {
  padding: 0 20px;

  .check-items-header {
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-summary {
      font-size: 13px;
      color: rgba(0, 0, 0, 0.65);

      .ant-divider-vertical {
        margin: 0 12px;
      }
    }

    .header-actions {
      display: flex;
      align-items: center;
    }
  }
}

.ant-list-item-meta {
  align-items: center;
}

.ant-list-item {
  padding: 12px !important;
  
  &:hover {
    background-color: #f5f5f5;
  }
}

.ant-list-item-meta-title {
  margin-bottom: 0 !important;
  line-height: 22px !important;
}

.ant-list-item-meta-description {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}

.expand-icon-wrapper {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  user-select: none;
  color: #1890ff;
  white-space: nowrap;
  
  &:hover {
    opacity: 0.8;
  }

  .expand-icon {
    font-size: 12px;
    margin-right: 4px;
    position: relative;
    top: 1px;
  }

  .expand-text {
    font-size: 13px;
    line-height: 1;
  }
}

.ant-table-row-expand-icon-cell {
  padding-left: 16px !important;
}

.action-buttons {
  position: relative;
  display: flex;  // 改回flex布局
  gap: 8px;  // 统一间距
  flex-wrap: nowrap;  // 不换行
  
  .ant-btn {
    padding: 0 8px;
    min-width: 48px;
    text-align: center;
    height: 24px;  // 统一按钮高度
    line-height: 22px;  // 统一文字行高
  }
}

// 主列表的操作按钮样式保持不变
.ant-table-row:not(.ant-table-expanded-row) .action-buttons {
  display: grid;
  grid-template-columns: repeat(3, 48px);
  gap: 8px;
  width: 160px;
}

.log-container {
  padding: 20px;
  height: calc(90vh - 150px);
  display: flex;
  flex-direction: column;
  position: relative;

  .log-header {
    margin-bottom: 16px;
    padding: 12px 16px;
    background-color: #fafafa;
    border: 1px solid #f0f0f0;
    border-radius: 4px;
    display: flex;
    flex-direction: column;
    flex-shrink: 0;
    gap: 16px;
    position: sticky;  // 添加sticky定位
    top: 0;          // 固定在顶部
    z-index: 10;     // 确保在内容之上
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
  
  .header-section {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 16px;
  }

  .log-content {
    flex: 1;
    overflow-y: auto;
    overflow-x: auto;
    padding: 16px;
    background-color: #ffffff;
    border: 1px solid #f0f0f0;
    border-radius: 4px;
    font-family: 'Consolas', 'Monaco', monospace;
    font-size: 14px;
    line-height: 1.5;
    color: #000000;
    position: relative;
    display: flex;
    flex-direction: column;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    margin-top: 8px;  // 添加顶部间距
    
    pre {
      white-space: pre-wrap;
      word-wrap: break-word;
      margin: 0;
      overflow-x: visible;
      flex: 1;
      padding: 0;
    }

    .no-log {
      text-align: center;
      padding: 12px;
      color: rgba(0, 0, 0, 0.45);
    }
    
    /* 滚动条样式 */
    &::-webkit-scrollbar {
      width: 8px;
      height: 8px;
    }

    &::-webkit-scrollbar-track {
      background: #f5f5f5;
      border-radius: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: #e8e8e8;
      border-radius: 4px;
      
      &:hover {
        background: #d9d9d9;
      }
    }

    .log-source {
      cursor: pointer;
      transition: all 0.3s;
      padding: 2px 6px;
      border-radius: 2px;
      
      &:hover {
        background-color: #f5f5f5;
        color: #1890ff !important;
      }
      
      &:active {
        background-color: #e6f7ff;
      }
    }
  }
}

:global(.log-modal) {
  top: 5vh;
  
  .ant-modal-body {
    position: relative;  // 添加相对定位
    padding: 0;         // 移除默认内边距
    max-height: 90vh;   // 限制最大高度
    overflow: hidden;   // 隐藏溢出内容
  }
}

.refresh-options {
  display: flex;
  align-items: center;
  gap: 12px;

  .refresh-btn, .auto-refresh-btn {
    height: 32px;
    border-radius: 4px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    
    .anticon {
      font-size: 14px;
      margin-right: 6px;
    }
  }

  .refresh-btn {
    min-width: 100px;
    padding: 0 16px;
    background: #1890ff;
    border-color: #1890ff;
    
    &:hover {
      background: #40a9ff;
      border-color: #40a9ff;
    }
  }

  .auto-refresh-btn {
    min-width: 130px;
    padding: 0 12px;
    background: #fff;
    border-color: #d9d9d9;
    color: rgba(0, 0, 0, 0.65);

    &.ant-btn-primary {
      background: #1890ff;
      border-color: #1890ff;
      color: #fff;
    }
    
    .anticon-down {
      font-size: 12px;
      margin-left: 4px;
      margin-right: 0;
    }
  }
}

.log-type-selector {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  
  .filter-title {
    white-space: nowrap;
    margin-right: 10px;
    font-size: 14px;
    color: rgba(0, 0, 0, 0.85);
  }
  
  .ant-radio-group {
    display: flex;
    
    .ant-radio-button-wrapper {
      text-align: center;
      min-width: 80px;
    }
  }
}

.combined-filter-status {
  margin: 4px 0;
  padding: 8px 12px;
  background-color: #f9f9f9;
  border-radius: 4px;
  border-left: 3px solid #1890ff;
  width: 100%;
}

.filter-description {
  margin: 0;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  
  .highlight {
    color: #1890ff;
    font-weight: 500;
  }
}

/* 主表格行高控制 */
:deep(.host-check-table) {
  .ant-table-thead > tr > th {
    padding-top: 8px !important;
    padding-bottom: 8px !important;
    line-height: 1.2 !important;
    white-space: nowrap !important;
  }
  
  .ant-table-tbody > tr > td {
    padding-top: 12px !important;
    padding-bottom: 12px !important;
    line-height: 1.5 !important;
  }
}

.log-filter-container {
  margin-bottom: 0;
  padding: 0;
  background-color: transparent;
  display: flex;
  align-items: center;
  flex-grow: 1;
}

.reset-filter {
  display: none; /* 隐藏原有的重置筛选按钮 */
}

.log-type-selector {
  display: flex;
  align-items: center;
  
  .filter-title {
    white-space: nowrap;
    margin-right: 10px;
    font-size: 14px;
    color: rgba(0, 0, 0, 0.85);
  }
  
  .ant-radio-group {
    display: flex;
    
    .ant-radio-button-wrapper {
      text-align: center;
      min-width: 80px;
    }
  }
}

.filter-area {
  background-color: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #eee;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
}

/* 确认弹窗样式定制 - 现代美化版本 */
:global(.check-item-confirm-modal) {
  min-height: 220px;
  
  .ant-modal-content {
    border-radius: 12px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
    overflow: visible;
    min-height: 220px;
    border: 1px solid rgba(24, 144, 255, 0.1);
    background: rgba(255, 255, 255, 0.98);
    transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
    
    &:hover {
      box-shadow: 0 12px 36px rgba(0, 0, 0, 0.15);
    }
  }
  
  .ant-modal-body {
    padding: 0;
  }
  
  // 在这里删除一些可能干扰焦点管理的CSS属性
  .ant-modal-confirm-body-wrapper {
    padding: 0;
    min-height: 220px;
    display: flex;
    flex-direction: column;
  }
  
  .ant-modal-confirm-body {
    flex: 1;
    display: flex;
    flex-direction: column;
    padding: 24px 24px 0;
    
    // 不要完全隐藏图标，只是调整显示
    .anticon {
      visibility: hidden;
      position: absolute;
      width: 0;
      height: 0;
    }
    
    .ant-modal-confirm-title {
      font-size: 16px;
      font-weight: 600;
      color: rgba(0, 0, 0, 0.85);
      line-height: 1.4;
      display: block;
      margin-bottom: 16px;
      padding: 0;
    }
    
    // 在这里调整内容区域的样式，确保正确处理焦点
    .ant-modal-confirm-content {
      word-break: break-all;
      white-space: pre-wrap;
      min-height: 120px;
      max-height: 320px;
      overflow: auto;
      font-size: 14px;
      line-height: 1.6;
      color: rgba(0, 0, 0, 0.85);
      margin: 0 !important;
      padding: 0 !important;
      width: 100% !important;
      outline: none; // 移除可能导致焦点问题的outline
      
      /* 自定义滚动条样式 */
      &::-webkit-scrollbar {
        width: 6px;
        height: 6px;
      }
      
      &::-webkit-scrollbar-track {
        background: #f5f5f5;
        border-radius: 3px;
      }
      
      &::-webkit-scrollbar-thumb {
        background: #ddd;
        border-radius: 3px;
        
        &:hover {
          background: #ccc;
        }
      }
    }
  }
  
  // 确保按钮区域的可访问性
  .ant-modal-confirm-btns {
    margin-top: 0;
    display: flex;
    justify-content: flex-end;
    width: 100%;
    padding: 16px 24px 24px;
    
    .ant-btn {
      height: 40px;
      padding: 0 24px;
      font-size: 14px;
      font-weight: 500;
      border-radius: 8px;
      margin-left: 12px;
      // 移除以下可能导致焦点问题的属性
      // visibility: visible !important;
      // opacity: 1 !important;
      // display: inline-flex !important;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      transition: all 0.25s ease-in-out;
      position: relative;
      overflow: hidden;
      
      &::after {
        content: '';
        position: absolute;
        top: 50%;
        left: 50%;
        width: 5px;
        height: 5px;
        background: rgba(255, 255, 255, 0.5);
        opacity: 0;
        border-radius: 100%;
        transform: scale(1, 1) translate(-50%, -50%);
        transform-origin: 50% 50%;
      }
      
      &:focus:not(:active)::after {
        animation: ripple 0.6s ease-out;
      }
    }
    
    // 保留按钮样式但确保它们可以正确接收焦点
    .ant-btn-primary {
      background-color: #1890ff;
      border-color: #1890ff;
      box-shadow: 0 2px 6px rgba(24, 144, 255, 0.4);
      
      &:hover {
        background-color: #40a9ff;
        border-color: #40a9ff;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(24, 144, 255, 0.45);
      }
      
      &:active {
        transform: translateY(0);
        box-shadow: 0 2px 4px rgba(24, 144, 255, 0.4);
      }
      
      // 添加焦点样式
      &:focus {
        outline: 2px solid rgba(24, 144, 255, 0.3);
        outline-offset: 1px;
      }
    }
    
    .ant-btn-danger {
      background-color: #ff4d4f;
      border-color: #ff4d4f;
      color: #fff;
      box-shadow: 0 2px 6px rgba(255, 77, 79, 0.4);
      
      &:hover {
        background-color: #ff7875;
        border-color: #ff7875;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(255, 77, 79, 0.45);
      }
      
      &:active {
        transform: translateY(0);
        box-shadow: 0 2px 4px rgba(255, 77, 79, 0.4);
      }
      
      // 添加焦点样式
      &:focus {
        outline: 2px solid rgba(255, 77, 79, 0.3);
        outline-offset: 1px;
      }
    }
    
    .confirm-fix-btn {
      position: relative;
      
      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: linear-gradient(45deg, rgba(255,255,255,0) 0%, rgba(255,255,255,0.1) 100%);
        background-size: 200% 200%;
        animation: shimmer 2s infinite linear;
      }
    }
    
    .cancel-fix-btn {
      border-color: #d9d9d9;
      
      &:hover {
        color: #40a9ff;
        border-color: #40a9ff;
        background-color: rgba(24, 144, 255, 0.04);
        transform: translateY(-2px);
      }
      
      &:active {
        transform: translateY(0);
      }
      
      // 添加焦点样式
      &:focus {
        outline: 2px solid rgba(0, 0, 0, 0.1);
        outline-offset: 1px;
      }
    }
  }
}

/* 自定义确认对话框组件样式 */
.modern-confirm-dialog {
  .modal-custom-title {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
    
    .title-icon {
      margin-right: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 28px;
      height: 28px;
      background: #1890ff;
      border-radius: 6px;
      color: white;
      font-size: 16px;
    }
    
    .title-text {
      font-size: 18px;
      font-weight: 600;
      color: rgba(0, 0, 0, 0.85);
    }
  }
  
  .modal-content-wrapper {
    display: flex;
    padding: 0;
    
    .warning-icon-container {
      flex-shrink: 0;
      margin-right: 16px;
      position: relative;
      width: 40px;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      
      .warning-icon {
        font-size: 28px;
        color: #faad14;
        z-index: 2;
        animation: pulse 2s infinite ease-in-out;
      }
      
      .pulse-ring {
        position: absolute;
        width: 100%;
        height: 100%;
        border-radius: 50%;
        background-color: rgba(250, 173, 20, 0.2);
        z-index: 1;
        animation: pulse-ring 2s ease-out infinite;
      }
    }
    
    .confirmation-content {
      flex: 1;
      padding: 16px;
      background-color: #fff7e6;
      border: 1px solid #ffe58f;
      border-radius: 8px;
      word-break: break-all;
      white-space: pre-wrap;
      line-height: 1.6;
      overflow: auto;
      max-height: 300px;
      margin-left: 0 !important;
      transition: all 0.3s ease;
      box-shadow: 0 2px 8px rgba(250, 173, 20, 0.1);
      
      &:hover {
        box-shadow: 0 4px 12px rgba(250, 173, 20, 0.15);
      }
    }
  }
}

/* 波纹动画效果 */
@keyframes ripple {
  0% {
    transform: scale(0, 0);
    opacity: 1;
  }
  20% {
    transform: scale(25, 25);
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: scale(40, 40);
  }
}

/* 闪光效果 */
@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

/* 脉冲效果 */
@keyframes pulse {
  0% {
    transform: scale(0.95);
  }
  50% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(0.95);
  }
}

@keyframes pulse-ring {
  0% {
    transform: scale(0.8);
    opacity: 0.8;
  }
  70% {
    transform: scale(1.2);
    opacity: 0;
  }
  100% {
    transform: scale(0.8);
    opacity: 0;
  }
}

/* 删除之前所有复杂的确认弹窗样式，重新编写简单明了的样式 */
:global(.fix-confirm-dialog) {
  .ant-modal-content {
    border-radius: 8px;
    overflow: hidden;
  }
  
  .ant-modal-body {
    padding: 24px;
  }
  
  .ant-modal-confirm-body {
    .ant-modal-confirm-title {
      color: rgba(0, 0, 0, 0.85);
      font-weight: 600;
      font-size: 16px;
      line-height: 1.4;
      margin-bottom: 16px;
    }
    
    .anticon {
      color: #faad14;
      font-size: 22px;
      margin-right: 16px;
      position: relative;
      top: 0;
    }
    
    .ant-modal-confirm-content {
      margin: 8px 0 0 38px;
      padding: 12px 16px;
      background-color: #fff7e6;
      border: 1px solid #ffe58f;
      border-radius: 4px;
      font-size: 14px;
      line-height: 1.6;
      color: rgba(0, 0, 0, 0.85);
      max-height: 300px;
      overflow-y: auto;
      word-break: break-all;
      white-space: pre-wrap;
      
      /* 美化滚动条 */
      &::-webkit-scrollbar {
        width: 6px;
        height: 6px;
      }
      
      &::-webkit-scrollbar-track {
        background: #f5f5f5;
        border-radius: 3px;
      }
      
      &::-webkit-scrollbar-thumb {
        background: #ddd;
        border-radius: 3px;
        
        &:hover {
          background: #ccc;
        }
      }
    }
  }
  
  .ant-modal-confirm-btns {
    margin-top: 24px;
    
    .ant-btn {
      height: 32px;
      min-width: 80px;
      padding: 0 15px;
      font-size: 14px;
      border-radius: 4px;
      margin-left: 8px;
    }
    
    .ant-btn-primary {
      background-color: #1890ff;
      border-color: #1890ff;
      
      &:hover {
        background-color: #40a9ff;
        border-color: #40a9ff;
      }
    }
    
    .ant-btn-danger {
      background-color: #ff4d4f;
      border-color: #ff4d4f;
      color: #fff;
      
      &:hover {
        background-color: #ff7875;
        border-color: #ff7875;
      }
    }
  }
}

.fix-confirm-modal {
  .ant-modal-content {
    border-radius: 8px;
    overflow: hidden;
  }
  
  .ant-modal-header {
    padding: 16px 24px;
    border-bottom: 1px solid #f0f0f0;
    background-color: #fff;
  }
  
  .ant-modal-title {
    font-size: 16px;
    line-height: 22px;
    font-weight: 500;
    color: rgba(0, 0, 0, 0.85);
  }
  
  .ant-modal-body {
    padding: 24px;
  }

  .fix-confirm-content {
    padding: 16px;
    background-color: #fff7e6;
    border: 1px solid #ffe58f;
    border-radius: 4px;
    margin-bottom: 16px;
    font-size: 14px;
    line-height: 1.6;
    color: rgba(0, 0, 0, 0.85);
    word-break: break-all;
    white-space: pre-wrap;
    
    /* 自定义滚动条样式 */
    &::-webkit-scrollbar {
      width: 6px;
      height: 6px;
    }
    
    &::-webkit-scrollbar-track {
      background: #f5f5f5;
      border-radius: 3px;
    }
    
    &::-webkit-scrollbar-thumb {
      background: #ddd;
      border-radius: 3px;
      
      &:hover {
        background: #ccc;
      }
    }
  }
  
  .ant-modal-footer {
    padding: 10px 24px;
    border-top: 1px solid #f0f0f0;
    
    .ant-btn {
      height: 32px;
      padding: 0 15px;
      font-size: 14px;
    }
    
    .ant-btn + .ant-btn {
      margin-left: 8px;
    }
    
    .ant-btn-primary {
      background-color: #1890ff;
      border-color: #1890ff;
      
      &:hover {
        background-color: #40a9ff;
        border-color: #40a9ff;
      }
    }
    
    .ant-btn-danger {
      background-color: #ff4d4f;
      border-color: #ff4d4f;
      color: #fff;
      
      &:hover {
        background-color: #ff7875;
        border-color: #ff7875;
      }
    }
  }
}

/* 修复中状态样式 */
.fixing-status {
  color: #1890ff;
  background-color: #e6f7ff;
  border-color: #91d5ff;
}
</style>