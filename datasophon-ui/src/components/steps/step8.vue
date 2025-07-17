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


 * @describe: step8-安装并启动服务
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2023-03-15 10:37:53
 * @FilePath: \ddh-ui\src\components\steps\step8.vue
-->
<template>
  <div class="steps8 steps apple-style-container">
    <div class="steps-header">
      <div class="header-content">
        <div class="back-button" v-if="currentPage !== 1" @click="goBack">
          <a-icon type="left" />
          <span>返回</span>
        </div>
        <div class="title-section">
          <h1 class="page-title">{{title}}</h1>
          <p class="page-subtitle" v-if="currentPage === 1">监控服务安装和启动进度</p>
        </div>
      </div>
      
      <a-button 
        @click="handleCancel" 
        class="apple-button close-button"
      >
        <a-icon type="close" />
      </a-button>
    </div>

    <div class="content-container">
      <!-- 命令列表表格 -->
      <a-table 
        v-if="currentPage === 1" 
        @change="tableChange" 
        :columns="columns" 
        :loading="loading" 
        :dataSource="dataSource" 
        :scroll="{y: 500}" 
        :rowSelection="{
          selectedRowKeys: selectedRowKeys, 
          onChange: onSelectChange
        }" 
        rowKey="commandId" 
        :pagination="pagination"
        class="apple-table"
      />
      
      <!-- 主机列表表格 -->
      <a-table 
        v-if="[2,3].includes(currentPage)" 
        @change="tableChange" 
        :columns="columns" 
        :loading="loading" 
        :dataSource="dataSource" 
        :scroll="{y: 500}" 
        rowKey="hostCommandId" 
        :pagination="pagination"
        class="apple-table"
      />
      
      <!-- 日志查看组件 -->
      <LOGS 
        v-if="currentPage === 4" 
        :logData="logData" 
        :hideCancel="true"
        class="apple-logs" 
      />
    </div>
  </div>
</template>

<script>
import { mapActions, mapState } from "vuex";
import LOGS from "@/components/logs";

export default {
  inject: ["clusterId", "handleCancel"],
  props: {
    stepsType: {
      type: String,
      default: "cluster",
    },
  },
  components: { LOGS },
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
    columns() {
      let arr = [
        {
          title: "序号",
          key: "index",
          width: 120,
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
          title:
              this.currentPage === 1
                  ? "命令"
                  : this.currentPage === 2
                      ? "主机"
                      : "指令名称",
          key: this.currentPage === 2 ? "hostname" : "commandName",
          dataIndex: this.currentPage === 2 ? "hostname" : "commandName",
          width: 300,
          customRender: (text, row, index) => {
            return this.currentPage !== 3 ? (
                <span class={"command-name"} onClick={() => this.seeDetail(row)}>
                {text}
              </span>
            ) : (
                <span>{text}</span>
            );
          },
        },
        {
          title: "状态",
          key: "commandProgress",
          dataIndex: "commandProgress",
          customRender: (text, row, index) => {
            return (
                <span>
                {row.commandStateCode === 1 ? (
                    <a-progress
                        class="progress-warp"
                        percent={text}
                        status="active"
                    />
                ) : row.commandStateCode === 2 ? (
                    <a-progress class="progress-warp" percent={text} />
                ) : row.commandStateCode === 4 ? (
                    <a-progress class="progress-warp" strokeColor='#FFA53D' format={()=><a-icon style="color:#FFA53D" type="exclamation-circle" />} percent={text} />
                ) : (
                    <a-progress
                        class="progress-warp"
                        percent={text}
                        status="exception"
                    />
                )}
              </span>
            );
          },
        },
      ];
      if (this.currentPage === 1) {
        arr.push(
            {
              title: "开始时间",
              key: "createTime",
              dataIndex: "createTime",
              width: 180,
            },
            {
              title: "持续时间",
              key: "durationTime",
              dataIndex: "durationTime",
              width: 160,
            }
        );
      }
      if (this.currentPage === 3) {
        arr.push({
          title: "日志信息",
          key: "resultMsg",
          dataIndex: "resultMsg",
          // width: 140,
          customRender: (text, row, index) => {
            return (
                <span
                    class="flex-container command-name"
                    onClick={() => this.seeDetail(row)}
                >
                查看日志
              </span>
            );
          },
        });
      }
      return arr;
    },
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
  },
  mounted() {
    this.pollingSearch();
  },
  beforeDestroy() {
    clearInterval(this.timer1);
    clearInterval(this.timer2);
    clearInterval(this.timer3);
  },
};
</script>

<style lang="less" scoped>
// 苹果设计风格颜色变量
@apple-white: #ffffff;
@apple-blue: #0071e3;
@apple-blue-light: rgba(0, 113, 227, 0.1);
@apple-gray-100: #f5f5f7;
@apple-gray-200: #e5e5ea;
@apple-gray-300: #d2d2d7;
@apple-gray-400: #86868b;
@apple-gray-500: #6e6e73;
@apple-text: #1d1d1f;
@apple-border: rgba(0, 0, 0, 0.1);
@apple-success: #34c759;
@apple-warning: #ff9f0a;
@apple-error: #ff3b30;

.apple-style-container {
  font-family: "SF Pro Display", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen, Ubuntu, Cantarell, "Open Sans", sans-serif;
  color: @apple-text;
  margin: 0;
  padding: 0;
  background-color: @apple-white;
  min-height: 100%;
  position: relative;
}

.steps-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 24px 0;
  border-bottom: 1px solid @apple-border;
  margin-bottom: 24px;
  
  .header-content {
    display: flex;
    align-items: center;
    
    .back-button {
      display: flex;
      align-items: center;
      color: @apple-blue;
      font-size: 14px;
      cursor: pointer;
      margin-right: 16px;
      padding: 8px 12px;
      border-radius: 8px;
      transition: all 0.2s;
      
      &:hover {
        background-color: @apple-blue-light;
      }
      
      .anticon {
        margin-right: 4px;
      }
    }
    
    .title-section {
      .page-title {
        font-size: 24px;
        font-weight: 600;
        margin: 0 0 4px 0;
        color: @apple-text;
        background: linear-gradient(120deg, @apple-text, #505050);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }
      
      .page-subtitle {
        font-size: 14px;
        color: @apple-gray-500;
        margin: 0;
      }
    }
  }
  
  .apple-button {
    &.close-button {
      width: 32px;
      height: 32px;
      border: none;
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: @apple-gray-100;
      transition: all 0.2s;
      
      &:hover {
        background: @apple-gray-200;
      }
      
      .anticon {
        color: @apple-gray-500;
        font-size: 14px;
      }
    }
  }
}

.content-container {
  .apple-table {
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
    
    :deep(.ant-table-thead > tr > th) {
      background-color: @apple-gray-100;
      color: @apple-text;
      font-weight: 500;
      border-bottom: 1px solid @apple-border;
      padding: 16px;
      
      &:first-child {
        border-top-left-radius: 12px;
      }
      
      &:last-child {
        border-top-right-radius: 12px;
      }
    }
    
    :deep(.ant-table-tbody > tr > td) {
      border-bottom: 1px solid @apple-border;
      padding: 16px;
      transition: all 0.3s;
      
      .command-name {
        color: @apple-blue;
        cursor: pointer;
        transition: all 0.2s;
        
        &:hover {
          color: darken(@apple-blue, 10%);
          text-decoration: none;
        }
      }
      
      .progress-warp {
        margin: 8px 0;
        
        :deep(.ant-progress-bg) {
          background-color: @apple-blue;
        }
        
        :deep(.ant-progress-status-success .ant-progress-bg) {
          background-color: @apple-success;
        }
        
        :deep(.ant-progress-status-exception .ant-progress-bg) {
          background-color: @apple-error;
        }
        
        :deep(.ant-progress-text) {
          color: @apple-text;
          font-weight: 500;
        }
      }
    }
    
    :deep(.ant-table-tbody > tr) {
      &:hover > td {
        background-color: @apple-gray-100;
      }
      
      &:last-child > td {
        border-bottom: none;
        
        &:first-child {
          border-bottom-left-radius: 12px;
        }
        
        &:last-child {
          border-bottom-right-radius: 12px;
        }
      }
    }
    
    // 选择框样式
    :deep(.ant-checkbox-wrapper) {
      .ant-checkbox {
        .ant-checkbox-inner {
          border-radius: 4px;
          border-color: @apple-gray-300;
          transition: all 0.3s;
          
          &:hover {
            border-color: @apple-blue;
          }
        }
        
        &.ant-checkbox-checked .ant-checkbox-inner {
          background-color: @apple-blue;
          border-color: @apple-blue;
        }
      }
    }
  }
  
  // 分页器样式
  :deep(.ant-pagination) {
    margin-top: 20px;
    
    .ant-pagination-item {
      border-radius: 6px;
      border-color: @apple-gray-300;
      transition: all 0.3s;
      
      &:hover {
        border-color: @apple-blue;
        
        a {
          color: @apple-blue;
        }
      }
      
      &-active {
        border-color: @apple-blue;
        background-color: @apple-blue;
        
        a {
          color: white;
        }
        
        &:hover {
          border-color: darken(@apple-blue, 10%);
          background-color: darken(@apple-blue, 10%);
          
          a {
            color: white;
          }
        }
      }
    }
    
    .ant-pagination-prev,
    .ant-pagination-next {
      .ant-pagination-item-link {
        border-radius: 6px;
        transition: all 0.3s;
        
        &:hover {
          border-color: @apple-blue;
          color: @apple-blue;
        }
      }
    }
  }
  
  // 加载状态
  :deep(.ant-spin) {
    .ant-spin-dot {
      .ant-spin-dot-item {
        background-color: @apple-blue;
      }
    }
    
    .ant-spin-text {
      color: @apple-blue;
    }
  }
}

// 日志组件样式
.apple-logs {
  background: @apple-white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  padding: 24px;
  margin-top: 20px;
}

// 动画
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