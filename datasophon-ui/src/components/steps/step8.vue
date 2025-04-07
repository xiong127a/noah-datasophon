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


 * @describe: step8-安装并启动服务 
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2023-03-15 10:37:53
 * @FilePath: \ddh-ui\src\components\steps\step8.vue
-->
<template>
  <div class="steps8">
    <div class="hero-section">
      <h1 class="hero-title">部署完成</h1>
      <p class="hero-subtitle">您的集群已成功部署，可以开始使用了</p>
    </div>
    
    <div class="completion-container">
      <div class="success-animation">
        <div class="check-container">
          <a-icon type="check" class="check-icon" />
        </div>
      </div>
      
      <div class="service-summary">
        <h2 class="summary-title">部署摘要</h2>
        <div class="summary-content">
          <div class="summary-row">
            <span class="label">集群名称:</span>
            <span class="value">{{ clusterInfo.clusterName || '-' }}</span>
          </div>
          <div class="summary-row">
            <span class="label">集群ID:</span>
            <span class="value">{{ clusterInfo.id || '-' }}</span>
          </div>
          <div class="summary-row">
            <span class="label">集群框架:</span>
            <span class="value">{{ clusterInfo.frameType || '-' }}</span>
          </div>
          <div class="summary-row">
            <span class="label">部署模式:</span>
            <span class="value">{{ clusterInfo.deployType || '-' }}</span>
          </div>
          <div class="summary-row">
            <span class="label">服务数量:</span>
            <span class="value">{{ serviceList.length || '0' }}</span>
          </div>
          <div class="summary-row">
            <span class="label">主机数量:</span>
            <span class="value">{{ clusterInfo.hostNum || '0' }}</span>
          </div>
        </div>
      </div>
      
      <div class="service-list">
        <h2 class="list-title">已安装服务</h2>
        
        <a-table
          class="apple-table"
          :columns="columns"
          :data-source="serviceList"
          :pagination="false"
          :loading="loading"
          rowKey="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'status'">
              <div class="status-cell">
                <span class="status-dot" :class="getStatusClass(record.status)"></span>
                <span class="status-text">{{ getStatusText(record.status) }}</span>
              </div>
            </template>
            
            <template v-if="column.dataIndex === 'action'">
              <div class="action-cell">
                <a-button 
                  class="apple-button view" 
                  @click="viewDetail(record)"
                  size="small"
                >
                  <a-icon type="eye" />
                  查看详情
                </a-button>
                
                <a-button 
                  class="apple-button manage" 
                  @click="manageService(record)"
                  size="small"
                >
                  <a-icon type="setting" />
                  管理服务
                </a-button>
              </div>
            </template>
          </template>
        </a-table>
      </div>
      
      <div class="completion-actions">
        <a-button 
          class="apple-button primary"
          @click="goToOverview"
          size="large"
        >
          前往监控概览
        </a-button>
        
        <a-button 
          class="apple-button secondary"
          @click="goToServiceManage"
          size="large"
        >
          服务管理中心
        </a-button>
      </div>
    </div>
  </div>
</template>
<script>
import { mapActions, mapState } from "vuex";

export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  props: {
    stepsType: {
      type: String,
      default: "cluster",
    },
  },
  data() {
    return {
      hostType: "all",
      title: "安装并启动服务",
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
      timer1: null,
      timer2: null,
      timer3: null,
      loading: false,
      currentPage: 1,
      commandId: "", // 第二个列表请求页面需要的参数
      hostname: "", // 第三个列表请求页面需要的参数
      commandHostId: "", // 第三个列表请求页面需要的参数
      commandName: "",
      logData: "",
      clusterInfo: {},
      serviceList: [],
      columns: [
        {
          title: "序号",
          key: "index",
          width: 80,
          customRender: (text, row, index) => {
            return index + 1;
          },
        },
        {
          title: "服务名称",
          dataIndex: "serviceName",
          key: "serviceName",
        },
        {
          title: "版本",
          dataIndex: "serviceVersion",
          key: "serviceVersion",
        },
        {
          title: "状态",
          dataIndex: "status",
          key: "status",
        },
        {
          title: "操作",
          dataIndex: "action",
          key: "action",
          width: 220,
        },
      ],
    };
  },
  watch: {
    stepsType: {
      handler(val) {
        if (this.stepsType === "cluster-setting") {
          this.title = "后台操作";
        }
        if (this.stepsType === "service") {
          // this.currentSteps = 4
        }
      },
      immediate: true,
    },
  },
  computed: {
    ...mapState({
      steps: (state) => state.steps, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
      setting: (state) => state.setting
    }),
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
    getServiceList(flag) {
      if (!flag) this.loading = true;
      const params = {
        pageSize: this.pagination.pageSize,
        page: this.pagination.current,
        clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
      };
      if (this.currentPage === 2) params.commandId = this.commandId;
      if (this.currentPage === 3) {
        params.hostname = this.hostname;
        params.commandHostId = this.commandHostId;
      }
      const ajaxApi =
        this.currentPage === 1
          ? global.API.getServiceCommandlist
          : this.currentPage === 2
            ? global.API.getServiceHostList
            : global.API.getServiceRoleOrderList;
      // todo：这个接口地址需要替换
      this.$axiosPost(ajaxApi, params).then((res) => {
        this.loading = false;
        this.dataSource = res.data;
        this.pagination.total = res.total;
      });
    },
    //表格选择
    onSelectChange(selectedRowKeys, row) {
      let arr = row.filter((item) => item.commandStateCode !== 3);
      this.selectedRow = arr;
      this.selectedRowKeys = selectedRowKeys;
    },
    goBack() {
      clearInterval(this.timer1);
      clearInterval(this.timer2);
      clearInterval(this.timer3);
      this.currentPage--;
      this.loading = true;
      if (this.currentPage === 2) {
        this.title = this.commandName;
      }
      if (this.currentPage === 1) {
        this.title = "安装并启动服务";
      }
      if (this.currentPage === 3) {
        this.title = this.hostname;
      }
      this.dataSource = [];
      this.pagination.total = 0;
      this.pagination.current = 1;
      this.pollingSearch();
    },
    seeDetail(row) {
      clearInterval(this.timer1);
      clearInterval(this.timer2);
      clearInterval(this.timer3);
      this.pagination.current = 1;
      if (this.currentPage === 3) {
        this.loading = true;
        this.hostname = row.hostname;
        this.hostCommandId = row.hostCommandId;
        this.getLog();
        return false;
      }
      this.currentPage++;
      this.loading = true;
      if (this.currentPage === 2) {
        this.commandName = row.commandName;
        this.title = row.commandName;
        this.commandId = row.commandId;
      }
      if (this.currentPage === 3) {
        this.title = row.hostname;
        this.commandHostId = row.commandHostId;
        this.hostname = row.hostname;
      }
      this.dataSource = [];
      this.pagination.total = 0;
      this.pollingSearch();
    },
    getLog() {
      this.$axiosPost(global.API.getHostCommandLog, {
        hostCommandId: this.hostCommandId,
        clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
      }).then((res) => {
        this.loading = false
        this.logData = res.data;
        this.currentPage++;
        this.title = "查看日志";
      });
    },
    // 三秒去刷一下
    pollingSearch() {
      this.getServiceList(); // 先立马刷一次
      let self = this;
      if (self[`timer${this.currentPage}`])
        clearInterval(self[`timer${this.currentPage}`]);
      self[`timer${this.currentPage}`] = setInterval(() => {
        self.getServiceList(true);
      }, global.intervalTime);
    },
    // 重试
    retryHost(row) {
      let commandIds = "";
      if (row === "all") {
        if (this.selectedRowKeys.length < 1) {
          this.$message.warning("请至少选择一条命令！");
          return false;
        }
        if (this.selectedRow.length > 0) {
          this.$message.warning("目前只支持失败的命令进行重试操作！");
          return false;
        }
        commandIds = this.selectedRowKeys.join(",");
      } else {
        commandIds = row.commandId;
      }
      const params = {
        commandIds,
        commandType: this.steps.commandType,
        clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
      };
      this.$axiosPost(global.API.startExecuteCommand, params).then((res) => {
        this.selectedRowKeys = [];
        this.$message.success(`操作成功`);
        this.pollingSearch();
      });
    },
    // 取消
    cancelHost(row) {},
    // 主机环境校验是否完成 是否可以进入下一步
    async dispatcherHostAgentCompleted(callback) {
      const params = {
        clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
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
    routerTo() {
      // 实现路由到集群管理页面的逻辑
    },
    getClusterInfo() {
      this.loading = true;
      const params = {
        clusterId: this.clusterId,
      };
      
      this.$axiosPost(global.API.getClusterInfo, params).then((res) => {
        if (res.code === 200) {
          this.clusterInfo = res.data || {};
        }
        this.loading = false;
      });
    },
    getStatusClass(status) {
      switch (status) {
        case 'NORMAL':
          return 'running';
        case 'STOPPED':
          return 'stopped';
        case 'STARTING':
          return 'starting';
        case 'ERROR':
          return 'error';
        default:
          return 'unknown';
      }
    },
    getStatusText(status) {
      switch (status) {
        case 'NORMAL':
          return '运行中';
        case 'STOPPED':
          return '已停止';
        case 'STARTING':
          return '启动中';
        case 'ERROR':
          return '异常';
        default:
          return '未知';
      }
    },
    viewDetail(record) {
      this.$router.push({
        path: `/service-manage/service-list/${record.id}`,
      });
    },
    manageService(record) {
      this.$router.push({
        path: `/service-manage/service-list/${record.id}`,
      });
    },
    goToOverview() {
      this.handleCancel();
      this.$router.push('/overview');
    },
    goToServiceManage() {
      this.handleCancel();
      this.$router.push('/service-manage');
    },
  },
  mounted() {
    this.pollingSearch();
    this.getClusterInfo();
    this.getServiceList();
  },
  beforeDestroy() {
    clearInterval(this.timer1);
    clearInterval(this.timer2);
    clearInterval(this.timer3);
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

.steps8 {
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
  
  .completion-container {
    max-width: 1000px;
    margin: 0 auto;
    animation: slideUp 0.6s ease-out;
    animation-fill-mode: both;
    animation-delay: 0.2s;
    
    // 成功动画
    .success-animation {
      display: flex;
      justify-content: center;
      margin-bottom: 2rem;
      
      .check-container {
        width: 80px;
        height: 80px;
        border-radius: 50%;
        background-color: fadeout(@apple-green, 90%);
        display: flex;
        align-items: center;
        justify-content: center;
        animation: pulse 2s infinite;
        
        .check-icon {
          font-size: 42px;
          color: @apple-green;
          animation: bounceIn 0.6s;
        }
      }
    }
    
    // 服务摘要
    .service-summary {
      background-color: @apple-white;
      border-radius: 12px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
      margin-bottom: 2rem;
      padding: 1.5rem 2rem;
      
      .summary-title {
        .apple-font();
        font-size: 1.5rem;
        font-weight: 600;
        color: @apple-black;
        margin-bottom: 1.5rem;
      }
      
      .summary-content {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
        gap: 1.5rem;
        
        .summary-row {
          display: flex;
          align-items: center;
          
          .label {
            color: @apple-gray;
            font-size: 0.95rem;
            width: 100px;
            flex-shrink: 0;
          }
          
          .value {
            color: @apple-black;
            font-weight: 500;
            font-size: 1rem;
          }
        }
      }
    }
    
    // 服务列表
    .service-list {
      background-color: @apple-white;
      border-radius: 12px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
      margin-bottom: 2rem;
      padding: 1.5rem 2rem;
      
      .list-title {
        .apple-font();
        font-size: 1.5rem;
        font-weight: 600;
        color: @apple-black;
        margin-bottom: 1.5rem;
      }
      
      // 表格样式
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
      
      // 状态单元格样式
      .status-cell {
        display: flex;
        align-items: center;
        
        .status-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          margin-right: 8px;
          
          &.running {
            background-color: @apple-green;
            box-shadow: 0 0 6px @apple-green;
          }
          
          &.stopped {
            background-color: @apple-gray;
          }
          
          &.starting {
            background-color: @apple-blue;
            animation: pulse 1.5s infinite;
          }
          
          &.error {
            background-color: @apple-red;
          }
          
          &.unknown {
            background-color: @apple-yellow;
          }
        }
        
        .status-text {
          font-size: 14px;
        }
      }
      
      // 操作单元格样式
      .action-cell {
        display: flex;
        gap: 8px;
        
        .apple-button {
          border-radius: 15px;
          font-size: 13px;
          font-weight: 500;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          height: 30px;
          padding: 0 12px;
          transition: all 0.3s;
          
          .anticon {
            margin-right: 4px;
            font-size: 14px;
          }
          
          &.view {
            background-color: fadeout(@apple-blue, 90%);
            color: @apple-blue;
            border: 1px solid fadeout(@apple-blue, 70%);
            
            &:hover {
              background-color: fadeout(@apple-blue, 80%);
            }
          }
          
          &.manage {
            background-color: fadeout(@apple-gray, 90%);
            color: @apple-black;
            border: 1px solid fadeout(@apple-gray, 70%);
            
            &:hover {
              background-color: fadeout(@apple-gray, 80%);
            }
          }
        }
      }
    }
    
    // 完成后操作
    .completion-actions {
      display: flex;
      justify-content: center;
      gap: 16px;
      margin-top: 3rem;
      margin-bottom: 2rem;
      
      .apple-button {
        height: 48px;
        min-width: 180px;
        border-radius: 24px;
        font-size: 16px;
        font-weight: 500;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        transition: all 0.3s;
        
        &.primary {
          background-color: @apple-blue;
          border: none;
          color: white;
          
          &:hover {
            background-color: @apple-blue-hover;
          }
        }
        
        &.secondary {
          background-color: @apple-gray-light;
          border: none;
          color: @apple-black;
          
          &:hover {
            background-color: darken(@apple-gray-light, 5%);
          }
        }
      }
    }
  }
}

// 动画
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

@keyframes bounceIn {
  0% { transform: scale(0); opacity: 0; }
  60% { transform: scale(1.2); }
  100% { transform: scale(1); opacity: 1; }
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.7; }
  100% { transform: scale(1); opacity: 1; }
}
</style>