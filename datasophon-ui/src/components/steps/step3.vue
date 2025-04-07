<!--
/*
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


 * @describe: step3-主机Agent分发 
 * @Date: 2022-06-13 16:35:02 
 * @LastEditTime: 2022-07-01 15:16:56
 * @FilePath: \ddh-ui\src\components\steps\step3.vue
-->
<template>
  <div class="steps3">
    <div class="hero-section">
      <h1 class="hero-title">主机Agent分发</h1>
      <p class="hero-subtitle">确保所有主机成功安装必要的Agent程序，以便执行后续操作</p>
    </div>
    
    <div class="table-container">
      <div class="action-bar">
        <div></div>
        <a-button type="primary" class="apple-button primary" @click="retryHost('all')">
          <a-icon type="redo" />
          全部重试
        </a-button>
      </div>
      <a-table 
        @change="tableChange" 
        :columns="columns" 
        :loading="loading" 
        :dataSource="dataSource" 
        :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}" 
        rowKey="hostname" 
        :pagination="pagination"
        class="apple-table"
      ></a-table>
    </div>
    
    <!-- 日志悬浮卡片 -->
    <div 
      v-show="showLogCard" 
      class="log-card"
      :style="{
        left: logCardPosition.x + 'px',
        top: logCardPosition.y + 'px'
      }"
      @mouseenter="clearHideLogTimer"
      @mouseleave="handleMouseLeave"
    >
      <div class="log-card-header">
        <span class="log-card-title">{{ currentLogHost }} 最近日志</span>
        <a-icon type="close" @click="hideLog" />
      </div>
      <div class="log-card-body">
        <pre class="log-card-text">{{ currentLog || '暂无日志' }}</pre>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  data() {
    return {
      hostType: "all",
      selectedRowKeys: [],
      pagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showSizeChanger: true,
        pageSizeOptions: ["10", "20", "50", "100"],
        showTotal: (total) => `共 ${total} 条`,
      },
      dataSource: [],
      timer: null,
      loading: false,
      // 日志卡片相关数据
      currentLog: '',
      currentLogHost: null,
      hideLogTimer: null,
      showLogCard: false,
      logCardPosition: { x: 0, y: 0 },
      columns: [
        {
          title: "序号",
          key: "index",
          width: 80,
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
        { 
          title: "主机名", 
          key: "hostInfo", 
          dataIndex: "hostname", 
          width: 150,
          customRender: (text, row, index) => {
            const h = this.$createElement;
            
            return h('div', { class: 'host-info' }, [
              h('div', { class: 'hostname' }, [text || '未知主机名'])
            ]);
          } 
        },
        {
          title: "IP地址",
          key: "ip",
          dataIndex: "ip",
          width: 160
        },
        {
          title: "进度",
          key: "progress",
          dataIndex: "progress",
          width: 300,
          customRender: (text, row, index) => {
            const h = this.$createElement;
            
            // 根据安装状态设置不同的进度条样式
            const status = row.installStateCode === 1 ? "active" : 
                           row.installStateCode === 2 ? "success" : "exception";
            
            // 修复进度条显示问题
            return h('div', { class: 'progress-container' }, [
              h('a-progress', {
                props: {
                  percent: text,
                  status: status === "active" ? "active" : 
                          status === "success" ? "success" : "exception",
                  strokeWidth: 6,
                  showInfo: false
                },
                style: {
                  width: '80%',
                  marginRight: '12px'
                }
              }),
              h('div', { class: 'progress-info' }, [
                h('span', { class: `progress-percent progress-percent-${status}` }, [
                  `${text}%`
                ]),
                h('span', { class: `progress-icon progress-icon-${status}` }, [
                  status === "active" ? h('a-icon', { props: { type: 'loading' } }) :
                  status === "success" ? h('a-icon', { props: { type: 'check-circle', theme: 'filled' } }) :
                  h('a-icon', { props: { type: 'close-circle', theme: 'filled' } })
                ])
              ])
            ]);
          },
        },
        { 
          title: "状态信息", 
          key: "message", 
          dataIndex: "message", 
          customRender: (text, row, index) => {
            const h = this.$createElement;
            
            return h('div', { 
              class: 'message-container',
              on: {
                mouseenter: (event) => this.handleMouseEnter(event, row),
                mouseleave: () => this.handleMouseLeave()
              }
            }, [
              h('span', { 
                class: 'message-text',
                style: {
                  color: row.installStateCode === 3 ? '#FF453A' : 
                         row.installStateCode === 2 ? '#34C759' : '#007AFF'
                }
              }, [text || ''])
            ]);
          } 
        },
        {
          title: "操作",
          key: "action",
          width: 120,
          customRender: (text, row, index) => {
            const h = this.$createElement;
            
            // 创建重试按钮
            const retryButton = h('button', {
              class: row.installStateCode === 3 ? 
                    'apple-action-button retry-button' : 
                    'apple-action-button retry-button disabled',
              attrs: {
                disabled: row.installStateCode !== 3
              },
              on: {
                click: () => this.retryHost(row)
              }
            }, [
              h('a-icon', { props: { type: 'redo' } }),
              h('span', ['重试'])
            ]);
            
            return h('div', { class: 'action-buttons' }, [retryButton]);
          },
        },
      ],
    };
  },
  methods: {
    changeType(type) {
      this.hostType = type;
    },
    tableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize
      this.pollingSearch();
    },
    getAgentList(flag) {
      if (!flag) this.loading = true;
      const params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
        clusterId: this.clusterId,
      };
      // todo：这个接口地址需要替换
      this.$axiosPost(global.API.dispatcherHostAgentList, params).then(
        (res) => {
          this.loading = false;
          this.dataSource = res.data;
          this.pagination.total = res.total;
        }
      );
    },
    //表格选择
    onSelectChange(selectedRowKeys, row) {
      let arr = row.filter(item => item.installStateCode !== 3)
      this.selectedRow = arr
      this.selectedRowKeys = selectedRowKeys;
    },
    // 三秒去刷一下
    pollingSearch() {
      this.getAgentList(); // 先立马刷一次
      let self = this;
      if (self.timer) clearInterval(self.timer);
      self.timer = setInterval(() => {
        self.getAgentList(true);
      }, global.intervalTime);
    },
    // 重试
    retryHost(row) {
      let ips = "";
      if (row === "all") {
        if (this.selectedRowKeys.length < 1) {
          this.$message.warning("请至少选择一台主机！");
          return false;
        }
        if (this.selectedRow.length > 0) {
          this.$message.warning("目前只支持失败的主机进行重试操作！");
          return false;
        }
        // 获取选中行的IP地址
        const selectedIPs = this.dataSource
          .filter(item => this.selectedRowKeys.includes(item.hostname))
          .map(item => item.ip);
        ips = selectedIPs.join(",");
      } else {
        ips = row.ip;
      }
      const params = {
        ips,
        clusterId: this.clusterId,
      };
      this.$axiosPost(global.API.reStartDispatcherHostAgent, params).then(
        (res) => {
          this.selectedRowKeys = [];
          this.$message.success(`操作成功`);
          this.pollingSearch();
        }
      );
    },
    
    // 处理鼠标进入
    async handleMouseEnter(event, row) {
      // 清除之前的定时器
      this.clearHideLogTimer();
      
      // 设置卡片位置
      this.updateCardPosition(event);
      
      // 如果已经在显示这个主机的日志，就不需要重新加载
      if (this.currentLogHost === row.hostname && this.showLogCard) {
        return;
      }
      
      this.currentLogHost = row.hostname;
      this.currentLog = '加载中...';
      this.showLogCard = true;
      
      try {
        const params = {
          ip: row.ip,
          clusterId: this.clusterId
        };
        
        const res = await this.$axiosGet(global.API.getWorkerLog, params);
        if (res && res.data) {
          this.currentLog = res.data;
        } else {
          this.currentLog = '获取日志失败';
        }
      } catch (error) {
        this.currentLog = '获取日志失败: ' + error.message;
      }
    },
    
    // 更新卡片位置
    updateCardPosition(event) {
      // 获取视窗宽度和高度
      const windowWidth = window.innerWidth;
      const windowHeight = window.innerHeight;
      
      // 获取事件触发位置
      const x = event.clientX;
      const y = event.clientY;
      
      // 设置卡片宽高，用于计算溢出
      const cardWidth = 600;
      const cardHeight = 400;
      
      // 计算卡片位置，避免超出视窗
      let posX = x + 20; // 默认在鼠标右侧20px处
      let posY = y;
      
      // 如果卡片会超出右侧边界，则显示在左侧
      if (posX + cardWidth > windowWidth) {
        posX = x - cardWidth - 20;
      }
      
      // 如果卡片会超出底部边界，则向上调整
      if (posY + cardHeight > windowHeight) {
        posY = windowHeight - cardHeight - 10;
      }
      
      // 确保卡片不会超出顶部
      if (posY < 10) {
        posY = 10;
      }
      
      this.logCardPosition = { x: posX, y: posY };
    },
    
    // 清除隐藏定时器
    clearHideLogTimer() {
      if (this.hideLogTimer) {
        clearTimeout(this.hideLogTimer);
        this.hideLogTimer = null;
      }
    },
    
    // 处理鼠标离开
    handleMouseLeave() {
      // 清除之前的定时器
      this.clearHideLogTimer();
      
      // 设置1秒后隐藏日志
      this.hideLogTimer = setTimeout(() => {
        this.hideLog();
      }, 1000);
    },
    
    // 隐藏日志
    hideLog() {
      this.showLogCard = false;
      this.clearHideLogTimer();
    },
    
    // 主机环境校验是否完成 是否可以进入下一步
    async dispatcherHostAgentCompleted(callback) {
      const params = {
        clusterId: this.clusterId,
      };
      // 等待网络请求结束
      let flag = await this.$axiosPost(
        global.API.dispatcherHostAgentCompleted,
        params
      );
      // 网络请求结束后才执行下边的语句  如果传入的callback方法为空或者没传内容也不会去执行，这样也不会影响此方法在别处的调用
      if (callback) {
        callback(flag);
      }
    },
  },
  mounted() {
    this.pollingSearch();
  },
  beforeDestroy() {
    clearInterval(this.timer);
    this.clearHideLogTimer();
  },
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
@apple-red: #ff453a;
@apple-green: #34c759;
@apple-yellow: #ffd60a;
@apple-orange: #ff9f0a;

// 苹果设计系统字体
.apple-font() {
  font-family: "SF Pro Display", "SF Pro Icons", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}

.steps3 {
  margin: 0;
  max-width: 100%;
  background-color: @apple-white;
  overflow: hidden;
  animation: fadeIn 0.8s ease-out;
  
  .hero-section {
    text-align: center;
    margin-bottom: 2.5rem;
    position: relative;

    .hero-title {
      .apple-font();
      font-size: 2.5rem;
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
      margin: 0 auto 1.5rem;
      max-width: 600px;
    }
  }
  
  .table-container {
    border-radius: 12px;
    margin: 0 auto;
    max-width: 1200px;
    overflow: hidden;
    animation: slideUp 0.6s ease-out;
    animation-fill-mode: both;
    animation-delay: 0.2s;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  }
  
  .action-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding: 0 16px;
    
    .apple-button {
      height: 38px;
      padding: 0 18px;
      font-size: 14px;
      font-weight: 500;
      border-radius: 19px;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      display: flex;
      align-items: center;
      justify-content: center;
      
      .anticon {
        margin-right: 6px;
        font-size: 14px;
      }
      
      &.primary {
        background: @apple-blue;
        border: none;
        color: white;
        
        &:hover {
          background: @apple-blue-hover;
        }
      }
    }
  }
  
  // 自定义表格样式
  :deep(.apple-table) {
    .apple-font();
    
    .ant-table-thead > tr > th {
      background-color: @apple-gray-light;
      font-weight: 600;
      font-size: 0.95rem;
      color: @apple-black;
      padding: 16px 20px;
      border-bottom: 1px solid rgba(0,0,0,0.05);
      white-space: nowrap;
    }
    
    .ant-table-tbody > tr > td {
      padding: 14px 20px;
      border-bottom: 1px solid rgba(0,0,0,0.03);
      transition: background-color 0.3s;
    }
    
    .ant-table-tbody > tr:hover:not(.ant-table-expanded-row) > td {
      background-color: fadeout(@apple-gray-light, 50%);
    }
  }
  
  // 进度条容器样式
  .progress-container {
    width: 100%;
    padding: 2px 0;
    display: flex;
    align-items: center;
    
    .progress-info {
      display: flex;
      align-items: center;
      white-space: nowrap;
      min-width: 60px;
      
      .progress-percent {
        font-size: 13px;
        font-weight: 500;
        margin-right: 6px;
        
        &.progress-percent-active {
          color: @apple-blue;
        }
        
        &.progress-percent-success {
          color: @apple-green;
        }
        
        &.progress-percent-exception {
          color: @apple-red;
        }
      }
      
      .progress-icon {
        display: flex;
        align-items: center;
        font-size: 14px;
        
        &.progress-icon-active {
          color: @apple-blue;
        }
        
        &.progress-icon-success {
          color: @apple-green;
        }
        
        &.progress-icon-exception {
          color: @apple-red;
        }
      }
    }
  }
  
  // 覆盖ant-design原生进度条样式
  :deep(.ant-progress) {
    .ant-progress-outer {
      padding-right: 0;
    }
    
    .ant-progress-inner {
      background-color: rgba(0, 0, 0, 0.05);
    }
    
    .ant-progress-bg {
      height: 6px !important;
      
      &::after {
        height: 6px !important;
      }
    }
    
    &.ant-progress-status-active {
      .ant-progress-bg {
        background-color: @apple-blue;
        animation: progressPulse 2s infinite;
      }
    }
    
    &.ant-progress-status-success {
      .ant-progress-bg {
        background-color: @apple-green;
      }
    }
    
    &.ant-progress-status-exception {
      .ant-progress-bg {
        background-color: @apple-red;
      }
    }
  }
  
  // 进度条脉动动画
  @keyframes progressPulse {
    0% {
      opacity: 1;
    }
    50% {
      opacity: 0.7;
    }
    100% {
      opacity: 1;
    }
  }
  
  // 消息文本样式
  .message-text {
    .apple-font();
    font-size: 14px;
    display: block;
    line-height: 1.4;
  }
  
  // 主机信息样式
  .host-info {
    display: flex;
    
    .hostname {
      font-weight: 500;
      color: @apple-black;
    }
  }
  
  // 操作按钮样式
  .action-buttons {
    display: flex;
    gap: 8px;
    
    .apple-action-button {
      height: 30px;
      padding: 0 12px;
      border-radius: 15px;
      border: none;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;
      background-color: rgba(0, 0, 0, 0.05);
      color: @apple-black;
      
      .anticon {
        margin-right: 4px;
        font-size: 12px;
      }
      
      &:hover {
        background-color: rgba(0, 0, 0, 0.1);
      }
      
      &.retry-button {
        background-color: rgba(255, 69, 58, 0.1);
        color: @apple-red;
        
        &:hover {
          background-color: rgba(255, 69, 58, 0.2);
        }
        
        &.disabled {
          opacity: 0.5;
          cursor: not-allowed;
          
          &:hover {
            background-color: rgba(0, 0, 0, 0.05);
          }
        }
      }
    }
  }
  
  // 日志悬浮样式
  .message-container {
    position: relative;
    display: inline-block;
    cursor: pointer;
    
    &:hover {
      .message-text {
        text-decoration: underline;
      }
    }
  }
  
  // 日志悬浮卡片样式
  .log-card {
    position: fixed;
    z-index: 1000;
    width: 600px;
    max-height: 400px;
    background-color: @apple-white;
    border-radius: 12px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
    overflow: hidden;
    animation: fadeIn 0.2s ease;
    
    .log-card-header {
      padding: 14px 16px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      background-color: @apple-gray-light;
      border-bottom: 1px solid rgba(0, 0, 0, 0.05);
      
      .log-card-title {
        .apple-font();
        font-weight: 500;
        font-size: 15px;
        color: @apple-black;
      }
      
      .anticon {
        cursor: pointer;
        color: @apple-gray;
        transition: color 0.2s;
        font-size: 14px;
        
        &:hover {
          color: @apple-black;
        }
      }
    }
    
    .log-card-body {
      padding: 16px;
      max-height: 350px;
      overflow-y: auto;
      
      &::-webkit-scrollbar {
        width: 6px;
      }
      
      &::-webkit-scrollbar-track {
        background: @apple-gray-light;
        border-radius: 3px;
      }
      
      &::-webkit-scrollbar-thumb {
        background: @apple-gray;
        border-radius: 3px;
        
        &:hover {
          background: darken(@apple-gray, 10%);
        }
      }
      
      .log-card-text {
        margin: 0;
        font-family: 'SF Mono', 'Menlo', 'Monaco', 'Courier New', monospace;
        font-size: 13px;
        line-height: 1.5;
        color: @apple-black;
        white-space: pre-wrap;
        word-break: break-all;
      }
    }
  }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
</style>