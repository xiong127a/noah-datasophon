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
        <div class="log-content" v-loading="logLoading">
          <pre v-if="logContent">{{ logContent }}</pre>
          <div v-else class="no-log">暂无日志数据</div>
        </div>
      </div>
    </a-modal>
  </div>
</template>
<script>
export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  props: {
    steps1Data: Object,
    depType:String,
  },
  data() {
    return {
      selectedRowKeys: [],
      pagination: {
        total: 0,
        pageSize: 100,
        current: 1,
        showSizeChanger: true,
        pageSizeOptions: ["100", "200", "500", "1000"],
        showTotal: (total) => `共 ${total} 条`,
      },
      timer: null,
      isRequesting: false,
      dataSource: [],
      loading: false,
      firstDataLoaded: false,
      checkItemsMap: {}, // 存储每个主机的校验项
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
              MIXED: { text: '部分通过', color: '#faad14', icon: 'exclamation-circle' }
            };
            
            // 检查主机是否有检查项
            const checkItems = row.checkItems || [];
            
            // 找到当前检查的项目
            const currentItem = checkItems.find(item => item.status === 'CHECKING');
            
            // 找到等待检查的项目
            const waitingItem = checkItems.find(item => item.status === 'WAITING');
            
            // 如果都没有,显示最后一个检查项的状态
            const itemToShow = currentItem || waitingItem || (checkItems.length > 0 ? checkItems[checkItems.length - 1] : null);
            
            if (!itemToShow) return h('span', {}, ['-']);
            
            const status = statusMap[itemToShow.status] || statusMap.WAITING;
            
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
              h('span', {}, [itemToShow.itemName])
            ]);
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
              
              // 重试按钮 - 非检查中时显示
              !isChecking ? h('a-button', {
                attrs: {
                  type: 'link',
                  size: 'small',
                  disabled: !['FAILED', 'SUCCESS'].includes(row.status || row.statusStr)
                },
                on: {
                  click: () => this.retryCheck(row)
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
      refreshTimer: null
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
    startBatchCheckHosts(hosts) {
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
      this.$axiosJsonPost(global.API.batchCheckHosts + '?clusterId=' + this.clusterId, hostnamesToCheck)
        .then(res => {
          if (res.code === 200) {
            console.log('成功启动主机检查:', res.msg);
          } else {
            console.warn('启动主机检查失败:', res.msg);
          }
        })
        .catch(err => {
          console.error('调用批量检查API失败:', err);
        });
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
              'SKIPPED': { text: '已跳过', color: '#d9d9d9', icon: 'warning' }
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
          width: '20%'
        },
        {
          title: '操作',
          key: 'action',
          width: '25%',
          customRender: (text, row) => {
            const h = this.$createElement;
            const isChecking = row.status === 'CHECKING' || row.statusStr === 'CHECKING';
            const isFailed = row.status === 'FAILED' || row.statusStr === 'FAILED';
            
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
              
              // 重试按钮 - 非检查中时显示
              !isChecking ? h('a-button', {
                attrs: {
                  type: 'link',
                  size: 'small',
                  disabled: !((row.status === 'FAILED' || row.statusStr === 'FAILED' || 
                             row.status === 'SUCCESS' || row.statusStr === 'SUCCESS') && 
                             !(row.status === 'SKIPPED' || row.statusStr === 'SKIPPED'))
                },
                on: {
                  click: () => this.retryCheckItem(record.hostname, row.id)
                }
              }, ["重试"]) : null,
              
              // 修复按钮 - 失败时可用
              isFailed ? h('a-button', {
                attrs: {
                  type: 'link',
                  size: 'small'
                },
                on: {
                  click: () => this.fixCheckItem(record.hostname, row.id)
                }
              }, ["修复"]) : null,
              
              // 跳过按钮 - 失败时可用
              isFailed ? h('a-button', {
                attrs: {
                  type: 'link',
                  size: 'small'
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
                disabled: row.status === 'WAITING' || row.statusStr === 'WAITING'
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
          // 重新获取校验项状态
          setTimeout(() => {
            this.getHostCheckItems(hostname);
          }, 2000);
        }
      } catch (error) {
        console.error('重试检查项失败:', error);
        this.$message.error('重试检查项失败');
      }
    },

    // 跳过检查项
    async skipCheckItem(hostname, itemId) {
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
          const targetItem = items.find(item => item.id === itemId);
          if (targetItem) {
            targetItem.status = 'SKIPPED';
            this.$set(this.checkItemsMap, hostname, [...items]);
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

    // 修复单个检查项
    async fixCheckItem(hostname, itemId) {
      try {
        const res = await this.$axiosPost(global.API.fixCheckItem, {
          clusterId: this.clusterId,
          hostname: hostname,
          itemId: itemId
        });
        
        if (res.code === 200) {
          this.$message.success('修复指令已发送');
          this.refreshHostList();
        }
      } catch (error) {
        // 异常情况的日志记录，但不显示错误弹窗
        console.error('修复检查项失败:', error);
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
      try {
        const res = await this.$axiosPost(global.API.retryCheckItems, {
          clusterId: this.clusterId,
          hostname,
          itemNames: [itemId]  // 只重试选中的单个检查项
        });
        
        if (res.code === 200) {
          this.$message.success('重试指令已发送');
          // 重新获取校验项状态
          setTimeout(() => {
            this.getHostCheckItems(hostname);
          }, 2000);
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
      return selectedItems.some(itemId => {
        const item = items.find(i => i.id === itemId);
        return item && (item.status === 'FAILED' || item.status === 'SUCCESS') && item.status !== 'SKIPPED';
      });
    },

    /**
     * 停止主机检查
     */
    stopCheck(row) {
      this.$axiosPost(global.API.stopHostCheck, { 
        clusterId: this.clusterId,
        hostname: row.hostname
      }).then(res => {
        if (res.code === 200) {
          this.$message.success('已终止主机检查');
          this.refreshHostList();
        } else {
          this.$message.error(res.msg || '终止检查失败');
        }
      });
    },

    /**
     * 停止检查项
     */
    stopCheckItem(hostname, itemId) {
      if (!hostname || !itemId) {
        this.$message.error('参数错误：主机名和检查项ID不能为空');
        return;
      }

      // 将参数作为 URL 参数传递
      const params = new URLSearchParams({
        clusterId: this.clusterId,
        hostname: hostname,
        itemId: itemId
      });

      try {
        this.$axiosPost(global.API.stopCheckItem + '?' + params.toString())
          .then(res => {
            if (res && res.code === 200) {
              this.$message.success('已终止检查项');
              // 更新检查项状态
              const items = this.checkItemsMap[hostname];
              if (items) {
                const targetItem = items.find(item => item.id === itemId);
                if (targetItem) {
                  targetItem.status = 'WAITING';
                  this.$set(this.checkItemsMap, hostname, [...items]);
                }
              }
            }
            // 不再处理错误情况，因为拦截器已经显示了错误消息
          });
      } catch (error) {
        // 异常情况的日志记录，但不显示错误弹窗
        console.error('终止检查项失败:', error);
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

    // 添加查看日志按钮
    viewItemLog(hostname, itemId, itemName) {
      // 保存当前选择的检查项信息
      this.currentLogHostname = hostname;
      this.currentLogItemId = itemId;
      this.currentLogItemName = itemName;
      
      // 打开日志弹窗并加载日志
      this.logModalTitle = `日志 - 主机: ${hostname}, 检查项: ${itemName}`;
      this.logVisible = true;
      this.logContent = '';
      
      // 初始停止之前可能存在的自动刷新定时器
      this.stopAutoRefresh();
      
      // 获取日志数据
      this.fetchItemLog();
    },
    
    // 获取检查项日志
    async fetchItemLog() {
      if (!this.currentLogHostname || !this.currentLogItemId) {
        return;
      }
      
      this.logLoading = true;
      
      try {
        const res = await this.$axiosPost(global.API.getCheckItemLog, { 
          clusterId: this.clusterId,
          hostname: this.currentLogHostname,
          itemId: this.currentLogItemId
        });
        
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
    
    // 关闭日志查看弹窗
    closeLogModal() {
      this.logVisible = false;
      this.stopAutoRefresh();
      this.currentLogHostname = null;
      this.currentLogItemId = null;
      this.currentLogItemName = null;
    },
  },
  mounted() {
    // 直接开始轮询
    this.pollingSearch();
  },
  beforeDestroy() {
    // 清理定时器
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
    
    // 清理日志刷新定时器
    this.stopAutoRefresh();
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

  .log-header {
    margin-bottom: 16px;
    padding: 12px 16px;
    background-color: #fafafa;
    border: 1px solid #f0f0f0;
    border-radius: 4px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-shrink: 0;
  }

  .log-content {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    background-color: #1e1e1e;
    border-radius: 4px;
    font-family: 'Consolas', 'Monaco', monospace;
    font-size: 14px;
    line-height: 1.5;
    color: #d4d4d4;
    white-space: pre-wrap;
    word-wrap: break-word;

    .no-log {
      text-align: center;
      padding: 12px;
      color: rgba(255, 255, 255, 0.45);
    }

    &::-webkit-scrollbar {
      width: 8px;
      height: 8px;
    }

    &::-webkit-scrollbar-track {
      background: #2c2c2c;
      border-radius: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: #555;
      border-radius: 4px;
      
      &:hover {
        background: #666;
      }
    }
  }
}

:global(.log-modal) {
  top: 5vh;
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
</style>