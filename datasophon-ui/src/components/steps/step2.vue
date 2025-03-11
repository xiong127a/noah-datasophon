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
      <a-button type="primary" @click="retryEnvironment('all')">全部重试</a-button>
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
      dataSource: [],
      loading: false,
      checkItemsMap: {}, // 存储每个主机的校验项
      columns: [
        {
          title: "序号",
          key: "index",
          width: 70,
          customRender: (text, row, index) => {
            return (
              <span>
                {parseInt(
                  this.pagination.current === 1
                    ? index + 1
                    : index +
                        1 +
                        this.pagination.pageSize * (this.pagination.current - 1)
                )}
              </span>
            );
          },
        },
        { title: "主机", key: "hostname", dataIndex: "hostname" },
        {
          title: "当前受管",
          key: "managed",
          dataIndex: "managed",
          customRender: (text, row, index) => {
            return <span>{text ? "是" : "否"}</span>;
          },
        },
        {
          title: "检测结果",
          key: "phone",
          dataIndex: "phone",
          customRender: (text, row) => {
            // 获取该主机的所有检查项
            const checkItems = this.checkItemsMap[row.hostname] || [];
            
            // 如果还没有检查项数据,显示加载状态
            if (checkItems.length === 0) {
              return <span>-</span>;
            }

            // 使用字符串类型的状态映射
            const statusMap = {
              'WAITING': { color: '#1890ff', icon: 'clock-circle' },
              'SUCCESS': { color: '#52c41a', icon: 'check-circle' },
              'FAILED': { color: '#f5222d', icon: 'close-circle' },
              'CHECKING': { color: '#1890ff', icon: 'loading' },
              'SKIPPED': { color: '#faad14', icon: 'warning' }
            };

            // 找到当前正在检查的项目
            const currentItem = checkItems.find(item => 
              item.status === 'CHECKING'
            );

            // 如果没有正在检查的项目,找待检查的项目
            const waitingItem = !currentItem && checkItems.find(item => 
              item.status === 'WAITING'
            );

            // 如果都没有,显示最后一个检查项的状态
            const itemToShow = currentItem || waitingItem || checkItems[checkItems.length - 1];

            if (!itemToShow) return <span>-</span>;

            const status = statusMap[itemToShow.status];

            return (
              <span class="flex-container" style={{ display: 'flex', alignItems: 'center', color: status.color }}>
                <span style={{ marginRight: '4px' }}>{itemToShow.itemName}</span>
                <a-icon 
                  type={status.icon}
                  theme={!['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined}
                  twoToneColor={status.color}
                  style={{ fontSize: '14px' }}
                  spin={status.icon === 'loading'}
                />
              </span>
            );
          },
        },
        {
          title: "操作",
          key: "action",
          width: "15%",
          customRender: (text, row) => {
            return (
              <span class="action-buttons">
                <a-button
                  type="link"
                  size="small"
                  onClick={() => this.retryEnvironment(row)}
                >
                  重试
                </a-button>
              </span>
            );
          },
        },
      ],
      selectedCheckItems: {}, // 存储每个主机选中的检查项 { hostname: [itemName1, itemName2] }
    };
  },
  methods: {
    tableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize
      this.pollingSearch();
    },
    getEnvironmentList(flag) {
      if (!flag) this.loading = true;
      const params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
        clusterId: this.clusterId,
        ...this.steps1Data,
      };
      
      this.$axiosPost(global.API.analysisHostList, params).then((res) => {
        this.loading = false;
        if (res.code === 200) {
          this.dataSource = res.data;
          this.pagination.total = res.total;
          
          // 将返回的checkItems数据保存到checkItemsMap中
          if (res.data && res.data.length > 0) {
            res.data.forEach(host => {
              if (host.checkItems) {
                this.$set(this.checkItemsMap, host.hostname, host.checkItems);
              }
            });
          }
        }

        if (res.code === 200 && this.depType=='K8S'){
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
      });
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
            // 使用字符串类型的状态映射
            const statusMap = {
              'WAITING': { text: '待检查', color: '#1890ff', icon: 'clock-circle' },
              'SUCCESS': { text: '通过', color: '#52c41a', icon: 'check-circle' },
              'FAILED': { text: '未通过', color: '#f5222d', icon: 'close-circle' },
              'CHECKING': { text: '检查中', color: '#1890ff', icon: 'loading' },
              'SKIPPED': { text: '已跳过', color: '#faad14', icon: 'warning' }
            };

            const status = statusMap[text] || { text: '未知', color: '#999999', icon: 'question-circle' };

            return (
              <span style={{ color: status.color }}>
                <a-icon 
                  type={status.icon}
                  theme={!['loading', 'clock-circle'].includes(status.icon) ? "twoTone" : undefined}
                  twoToneColor={status.color}
                  style={{ marginRight: '4px' }}
                  spin={status.icon === 'loading'}
                />
                {status.text}
              </span>
            );
          }
        },
        {
          title: '检查结果',
          dataIndex: 'message',
          key: 'message',
          width: '40%'
        },
        {
          title: '操作',
          key: 'action',
          width: '20%',
          customRender: (text, row) => {
            return (
              <span class="action-buttons">
                <a-button 
                  type="link" 
                  size="small"
                  onClick={() => this.fixCheckItem(record.hostname, row.id)}
                  disabled={!row.canAutoFix || row.status !== 'FAILED'}
                >
                  修复
                </a-button>
                <a-button 
                  type="link" 
                  size="small"
                  onClick={() => this.skipCheckItem(record.hostname, row.id)}
                  disabled={row.status !== 'FAILED'}
                >
                  跳过
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  onClick={() => this.retryCheckItem(record.hostname, row.id)}
                  disabled={!(row.status === 'FAILED' || row.status === 'SUCCESS') || row.status === 'SKIPPED'}
                >
                  重试
                </a-button>
              </span>
            );
          }
        }
      ];

      return (
        <div class="check-items-container">
          <div class="check-items-header">
            <div class="header-summary">
              <span>共 {checkItems.length} 项检查</span>
              <a-divider type="vertical" />
              <span style={{ color: '#52c41a' }}>
                <a-icon type="check-circle" theme="twoTone" twoToneColor="#52c41a" style={{ marginRight: '4px' }} />
                {checkItems.filter(item => item.status === 'SUCCESS').length} 项通过
              </span>
              <a-divider type="vertical" />
              <span style={{ color: '#f5222d' }}>
                <a-icon type="close-circle" theme="twoTone" twoToneColor="#f5222d" style={{ marginRight: '4px' }} />
                {checkItems.filter(item => item.status === 'FAILED').length} 项失败
              </span>
              <a-divider type="vertical" />
              <span style={{ color: '#1890ff' }}>
                <a-icon type="clock-circle" style={{ marginRight: '4px' }} />
                {checkItems.filter(item => item.status === 'WAITING').length} 项待检查
              </span>
              <a-divider type="vertical" />
              <span style={{ color: '#1890ff' }}>
                <a-icon type="loading" style={{ marginRight: '4px' }} spin />
                {checkItems.filter(item => item.status === 'CHECKING').length} 项检查中
              </span>
              <a-divider type="vertical" />
              <span style={{ color: '#faad14' }}>
                <a-icon type="warning" theme="twoTone" twoToneColor="#faad14" style={{ marginRight: '4px' }} />
                {checkItems.filter(item => item.status === 'SKIPPED').length} 项已跳过
              </span>
            </div>
            <div class="header-actions">
              <a-button 
                type="primary" 
                size="small"
                onClick={() => this.retrySelectedItems(record.hostname)}
                disabled={!this.hasRetryableSelectedItems(record.hostname)}
                style={{ marginRight: '8px' }}
              >
                重试选中项
              </a-button>
              <a-button 
                type="primary" 
                size="small"
                onClick={() => this.fixSelectedItems(record.hostname)}
                disabled={!this.hasFixableSelectedItems(record.hostname)}
                style={{ marginRight: '8px' }}
              >
                修复选中项
              </a-button>
              <a-button 
                type="primary" 
                size="small"
                onClick={() => this.fixAllCheckItems(record.hostname)}
                disabled={!checkItems.some(item => item.status === 'FAILED')}
              >
                一键修复所有问题
              </a-button>
            </div>
          </div>
          <a-table
            columns={columns}
            dataSource={checkItems}
            pagination={false}
            size="middle"
            rowKey="id"
            rowSelection={{
              selectedRowKeys: this.selectedCheckItems[record.hostname] || [],
              onChange: (selectedRowKeys) => this.onCheckItemSelect(record.hostname, selectedRowKeys)
            }}
          />
        </div>
      );
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
          hostname,
          itemId
        });
        if (res.code === 200) {
          this.$message.success('修复指令已发送');
          // 通过轮询获取最新状态
          this.pollingSearch();
        }
      } catch (error) {
        console.error('修复检查项失败:', error);
        this.$message.error('修复检查项失败');
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
        const res = await this.$axiosPost(global.API.retryCheckItem, {
          clusterId: this.clusterId,
          hostname,
          itemId
        });
        if (res.code === 200) {
          this.$message.success('重试指令已发送');
          // 通过轮询获取最新状态
          this.pollingSearch();
        }
      } catch (error) {
        console.error('重试检查项失败:', error);
        this.$message.error('重试检查项失败');
      }
    },

    // 自定义展开图标
    customExpandIcon({ expanded, onExpand, record }) {
      return (
        <div class="expand-icon-wrapper" onClick={e => {
          onExpand(record, e);
        }}>
          <a-icon 
            type={expanded ? 'down' : 'right'} 
            class="expand-icon"
          />
          <span class="expand-text">
            {expanded ? '收起详情' : '查看详情'}
          </span>
        </div>
      );
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
        console.error('修复选中检查项失败:', error);
        this.$message.error('修复选中检查项失败');
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
  },
  mounted() {
    // 直接开始轮询
    this.pollingSearch();
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
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  
  .ant-btn {
    padding: 0 4px;
    min-width: 40px;
  }
}
</style>