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


 * @describe: step6-服务配置
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-10-31 16:00:46
 * @FilePath: \ddh-ui\src\components\steps\step7.vue
-->
<template>
  <div class="steps7">
    <div class="hero-section">
      <h1 class="hero-title">服务部署</h1>
      <p class="hero-subtitle">启动和部署所选服务，监控安装进度</p>
    </div>
    
    <div class="service-install-container">
      <div class="progress-overview">
        <div class="progress-cards">
          <div class="progress-card total">
            <div class="card-icon">
              <a-icon type="appstore" />
            </div>
            <div class="card-content">
              <div class="card-title">总服务数</div>
              <div class="card-value">{{ serviceTotal }}</div>
            </div>
          </div>
          
          <div class="progress-card success">
            <div class="card-icon">
              <a-icon type="check-circle" />
            </div>
            <div class="card-content">
              <div class="card-title">已安装</div>
              <div class="card-value">{{ successCount }}</div>
            </div>
          </div>
          
          <div class="progress-card pending">
            <div class="card-icon">
              <a-icon type="loading" />
            </div>
            <div class="card-content">
              <div class="card-title">安装中</div>
              <div class="card-value">{{ installingCount }}</div>
            </div>
          </div>
          
          <div class="progress-card failed">
            <div class="card-icon">
              <a-icon type="close-circle" />
            </div>
            <div class="card-content">
              <div class="card-title">安装失败</div>
              <div class="card-value">{{ failedCount }}</div>
            </div>
          </div>
        </div>
      </div>
      
      <div class="service-list">
        <a-table
          :columns="columns"
          :data-source="dataSource"
          :pagination="false"
          rowKey="id"
          :loading="loading"
          class="apple-table"
        >
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.dataIndex === 'status'">
              <span class="status-tag" :class="getStatusClass(record.status)">
                <a-icon :type="getStatusIcon(record.status)" />
                {{ getStatusText(record.status) }}
              </span>
            </template>
            
            <template v-if="column.dataIndex === 'progress'">
              <a-progress 
                :percent="record.progress" 
                :status="getProgressStatus(record.status)" 
                :stroke-color="getProgressColor(record.status)"
                class="apple-progress"
              />
            </template>
            
            <template v-if="column.dataIndex === 'action'">
              <a-button 
                v-if="record.status === 'FAILED'" 
                @click="retryService(record)"
                class="apple-button retry"
                size="small"
              >
                <a-icon type="redo" />
                重试
              </a-button>
              <a-button 
                v-if="record.status === 'FAILED'" 
                @click="showLog(record)"
                class="apple-button view-log"
                size="small"
              >
                <a-icon type="file-text" />
                查看日志
              </a-button>
            </template>
          </template>
        </a-table>
      </div>
    </div>
    
    <!-- 日志查看弹窗 -->
    <a-modal
      v-model="logVisible"
      title="安装日志"
      width="800px"
      :footer="null"
      class="apple-modal"
    >
      <div class="log-content" v-if="serviceLog">
        <pre>{{ serviceLog }}</pre>
      </div>
    </a-modal>
  </div>
</template>
<script>
import { mapActions, mapState } from "vuex";
import { de } from "date-fns/locale";

export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  props: {
    steps4Data: Object,
  },
  data() {
    return {
      loading: false,
      timer: null,
      dataSource: [],
      serviceTotal: 0,
      successCount: 0,
      installingCount: 0,
      failedCount: 0,
      logVisible: false,
      serviceLog: "",
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
          title: "状态",
          dataIndex: "status",
          key: "status",
        },
        {
          title: "进度",
          dataIndex: "progress",
          key: "progress",
        },
        {
          title: "操作",
          dataIndex: "action",
          key: "action",
          width: 200,
        },
      ],
    };
  },
  watch: {
    serviceContainerHeight(val) {
      console.log(val);
    },
  },
  computed: {
    ...mapState({
      steps: (state) => state.steps, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
      setting: (state) => state.setting, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
    }),
    serviceContainerHeight() {
      const className = this.serviceNameKey + "warp";
      const height = document.getElementsByClassName(className)[0];
      return height;
    },
  },
  methods: {
    templateProps(item) {
      return this.templateObj[item];
    },
    ...mapActions("steps", ["setCommandType", "setCommandIds"]),
    callback(key) {
      this.serviceNameKey = key;
      if (this.selectKeys.includes(key)) return false;
      this.selectKeys.push(key);
      // this.getServiceConfigOption();
    },
    // 去除字符串里面的数字
    deleteNum(str, key) {
      let reg = /[0-9]+/g;
      let str1 = str.replace(reg, "");
      let str2 = str1.replace(key, "");
      return str2;
    },
    handlearrayWithData(a) {
      let obj = {};
      let arr = [];
      for (let k in a) {
        if (k.includes("arrayWith")) {
          let key = "";
          if (k.includes("arrayWithKey")) {
            key = k.split("arrayWithKey")[0];
            arr.push(key);
          }
          if (k.includes("arrayWithVal")) {
            key = k.split("arrayWithVal")[0];
            arr.push(key);
          }
          arr = [...new Set(arr)];
        }
      }
      arr.map((item) => {
        obj[item] = [];
      });
      for (let f in obj) {
        let keys = [];
        let vals = [];
        for (let i in a) {
          if (i.includes(f)) {
            if (i.includes("arrayWithKey")) {
              keys.push(i);
            }
            if (i.includes("arrayWithVal")) {
              vals.push(i);
            }
          }
        }
        keys.map((item, index) => {
          obj[f].push({
            [`${a[item]}`]: a[vals[index]],
          });
        });
      }
      return obj;
    },
    handleMultipleData(a) {
      let obj = {};
      let arr = [];
      for (let k in a) {
        if (k.includes("multiple")) {
          let key = k.split("multiple")[0];
          arr.push(key);
          arr = [...new Set(arr)];
        }
      }
      arr.map((item) => {
        obj[item] = [];
      });
      // obj{ a: , b: }
      for (let f in obj) {
        let vals = [];
        for (let i in a) {
          if (i.includes(f)) {
            if (i.includes("multiple")) {
              vals.push(i);
            }
          }
        }
        vals.map((item, index) => {
          obj[f].push(a[vals[index]]);
        });
      }
      return obj;
    },
    // 单个标签页的保存
    handleSubmit() {
      this.templateData = this.templateObj[`${this.serviceNameKey}`];
      this.$refs[
        `CommonTemplateRef${this.serviceNameKey}`
      ][0].form.validateFields(async (err, values) => {
        if (!err) {
          let param = _.cloneDeep(this.templateData);
          const arrayWithData = this.handlearrayWithData(values);
          const multipleData = this.handleMultipleData(values);
          const formData = { ...values, ...arrayWithData, ...multipleData };
          for (let name in formData) {
            param.forEach((item) => {
              if (item.name === name) {
                item.value = formData[name];
              }
            });
          }
          param.forEach((item) => {
            item.name = item.name.replaceAll("!", ".");
          });
          let filterParam = param.filter(
            (item) => !(!item.required && item.hidden)
          );
          // 处理表单数据 将相同的key处理成数组
          let saveParam = {
            clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
            serviceName: this.serviceNameKey,
            serviceConfig: JSON.stringify(filterParam),
          };
          // // 等待网络请求结束
          let res = await this.$axiosPost(
            global.API.saveServiceConfig,
            saveParam
          );
          if (res.code === 200) {
            this.$message.success("保存成功");
          }
        }
      });
    },
    getServiceConfigOption() {
      this.loading = true;
      const self = this;
      this.SERVICENAMES.map((item) => {
        const params = {
          clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
          serviceName: item,
        };
        this.$axiosPost(global.API.getServiceConfigOption, params).then(
          (res) => {
            if (res.code === 200) {
              self.templateObj[item] = self.handlerTemplate(res.data);
              self.loading = false;
            }
            // self.templateData = this.handlerTemplate(res.data);
          }
        );
      });
    },
    handlerTemplate(data) {
      data.forEach((item) => {
        item.name = item.name.replaceAll(".", "!");
      });
      return data;
    },
    checkAllForm() {
      const self = this;
      let num = 0;
      for (let i = 0; i < self.SERVICENAMES.length; i++) {
        const item = self.SERVICENAMES[i];
        self.$refs[`CommonTemplateRef${item}`][0].form.validateFields(
          (err, values) => {
            if (err) {
              self.serviceNameKey = item;
              num++;
            }
          }
        );
        if (num > 0) break;
      }
      return num > 0;
    },
    submitAllServices(callback) {
      let promiseArr = [];
      this.SERVICENAMES.forEach((item) => {
        //todo 目前只有一个节点
        let p = null;
        p = new Promise((resolve) => {
          let serviceNameKey = item;
          this.templateData = this.templateObj[`${serviceNameKey}`];
          this.$refs[
            `CommonTemplateRef${serviceNameKey}`
          ][0].form.validateFields(async (err, values) => {
            if (!err) {
              let param = _.cloneDeep(this.templateData);
              const arrayWithData = this.handlearrayWithData(values);
              const multipleData = this.handleMultipleData(values);
              const formData = { ...values, ...arrayWithData, ...multipleData };
              for (let name in formData) {
                param.forEach((item) => {
                  if (item.name === name) {
                    item.value = formData[name];
                  }
                });
              }
              param.forEach((item) => {
                item.name = item.name.replaceAll("!", ".");
              });
              let filterParam = param.filter(
                (item) => !(!item.required && item.hidden)
              );
              // 处理表单数据 将相同的key处理成数组
              let saveParam = {
                clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
                serviceName: serviceNameKey,
                serviceConfig: JSON.stringify(filterParam),
              };
              // // 等待网络请求结束
              let res = await this.$axiosPost(
                global.API.saveServiceConfig,
                saveParam
              );
              resolve({ ...res, name: serviceNameKey });
            }
          });
        });
        if (p) promiseArr.push(p);
      });
      Promise.all(promiseArr).then(async (res) => {
        let num = 0;
        res.map((item) => {
          if (item.code !== 200) {
            this.$message.warnning(`${res.name}配置失败`);
            num++;
          }
        });
        if (num > 0) {
          let res = { code: 0 };
          callback(res)
          return false
        }
        let params = {
          clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
        };
        let a = false;
        if (a) {
          params.commandIds = this.steps.commandIds;
          params.commandType = this.steps.commandType;
          // 直接启动
          res = await this.$axiosPost(global.API.startExecuteCommand, params);
          if (callback) {
            callback(res);
          }
        } else {
          // 先调用生成指令再去启动
          params.serviceNames = this.SERVICENAMES;
          params.commandType = this.steps.commandType;
          let result = await this.$axiosPost(global.API.generateCommand, params);
          params.commandIds = result.data;
          this.setCommandIds(result.data);
          delete params.servicenames;
          res = await this.$axiosPost(global.API.startExecuteCommand, params);
          if (callback) {
            callback(res);
          }
        }
      });
    },
    //  从第七步进入第八步的请求
    async nextSteps(callback) {
      let res = { code: 0 };
      const flag = this.checkAllForm();
      if (flag && callback) {
        callback(res);
        return false;
      }
      // 如果所有的表单校验成功了 那么就把所有的tab页去保存一下
      this.submitAllServices(callback);
    },
    getStatusClass(status) {
      const statusMap = {
        'INSTALLING': 'installing',
        'SUCCESS': 'success',
        'FAILED': 'failed',
        'WAITING': 'waiting'
      };
      return statusMap[status] || 'default';
    },
    
    getStatusIcon(status) {
      const iconMap = {
        'INSTALLING': 'loading',
        'SUCCESS': 'check-circle',
        'FAILED': 'close-circle',
        'WAITING': 'clock-circle'
      };
      return iconMap[status] || 'question-circle';
    },
    
    getStatusText(status) {
      const textMap = {
        'INSTALLING': '安装中',
        'SUCCESS': '已安装',
        'FAILED': '安装失败',
        'WAITING': '等待安装'
      };
      return textMap[status] || '未知';
    },
    
    getProgressStatus(status) {
      const statusMap = {
        'INSTALLING': 'active',
        'SUCCESS': 'success',
        'FAILED': 'exception',
        'WAITING': 'normal'
      };
      return statusMap[status] || 'normal';
    },
    
    getProgressColor(status) {
      const colorMap = {
        'INSTALLING': '#0071e3',
        'SUCCESS': '#34c759',
        'FAILED': '#ff453a',
        'WAITING': '#86868b'
      };
      return colorMap[status] || '#86868b';
    },
    
    getInstallProgressList() {
      this.loading = true;
      const params = {
        clusterId: this.clusterId,
      };
      
      this.$axiosPost(global.API.getInstallProgressList, params).then((res) => {
        this.loading = false;
        if (res.code === 200) {
          this.dataSource = res.data;
          this.serviceTotal = this.dataSource.length;
          this.updateServiceCounts();
        }
      });
    },
    
    updateServiceCounts() {
      this.successCount = this.dataSource.filter(item => item.status === 'SUCCESS').length;
      this.failedCount = this.dataSource.filter(item => item.status === 'FAILED').length;
      this.installingCount = this.dataSource.filter(item => item.status === 'INSTALLING').length;
    },
    
    // 启动轮询
    startPolling() {
      this.getInstallProgressList();
      
      if (this.timer) {
        clearInterval(this.timer);
      }
      
      this.timer = setInterval(() => {
        this.getInstallProgressList();
        
        // 如果所有服务都已经完成安装（成功或失败），停止轮询
        if (this.installingCount === 0 && this.dataSource.length > 0) {
          this.stopPolling();
        }
      }, 5000);
    },
    
    stopPolling() {
      if (this.timer) {
        clearInterval(this.timer);
        this.timer = null;
      }
    },
    
    retryService(record) {
      const params = {
        clusterId: this.clusterId,
        serviceId: record.id,
      };
      
      this.loading = true;
      this.$axiosPost(global.API.retryServiceInstall, params).then((res) => {
        this.loading = false;
        if (res.code === 200) {
          this.$message.success('重试安装任务已提交');
          this.startPolling(); // 重新开始轮询
        } else {
          this.$message.error(res.msg || '重试失败');
        }
      });
    },
    
    showLog(record) {
      const params = {
        clusterId: this.clusterId,
        serviceId: record.id,
      };
      
      this.logVisible = true;
      this.serviceLog = "加载日志中...";
      
      this.$axiosPost(global.API.getServiceInstallLog, params).then((res) => {
        if (res.code === 200) {
          this.serviceLog = res.data || "没有找到安装日志";
        } else {
          this.serviceLog = "获取日志失败: " + (res.msg || "未知错误");
        }
      });
    },
  },
  created() {
    this.SERVICENAMES = this.steps4Data.serviceNames.map(
      (item) => item.serviceName
    );
    this.serviceNameKey = this.SERVICENAMES[0];
    this.selectKeys.push(this.serviceNameKey);
    this.SERVICENAMES.map((item) => {
      this.templateObj[`${item}`] = [];
    });
  },
  mounted() {
    this.startPolling();
  },
  beforeDestroy() {
    this.stopPolling();
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

.steps7 {
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
  
  .service-install-container {
    max-width: 1200px;
    margin: 0 auto;
    animation: slideUp 0.6s ease-out;
    animation-fill-mode: both;
    animation-delay: 0.2s;
    
    // 进度概览样式
    .progress-overview {
      margin-bottom: 2rem;
      
      .progress-cards {
        display: flex;
        justify-content: space-between;
        gap: 20px;
        flex-wrap: wrap;
        
        .progress-card {
          flex: 1;
          min-width: 220px;
          background-color: @apple-white;
          border-radius: 12px;
          box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
          padding: 20px;
          display: flex;
          align-items: center;
          transition: transform 0.3s, box-shadow 0.3s;
          
          &:hover {
            transform: translateY(-3px);
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
          }
          
          .card-icon {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 48px;
            height: 48px;
            border-radius: 12px;
            margin-right: 16px;
            
            .anticon {
              font-size: 24px;
            }
          }
          
          .card-content {
            .card-title {
              font-size: 14px;
              color: @apple-gray;
              margin-bottom: 6px;
            }
            
            .card-value {
              font-size: 28px;
              font-weight: 600;
              line-height: 1;
            }
          }
          
          &.total {
            .card-icon {
              background-color: fadeout(@apple-blue, 90%);
              color: @apple-blue;
            }
            
            .card-value {
              color: @apple-blue;
            }
          }
          
          &.success {
            .card-icon {
              background-color: fadeout(@apple-green, 90%);
              color: @apple-green;
            }
            
            .card-value {
              color: @apple-green;
            }
          }
          
          &.pending {
            .card-icon {
              background-color: fadeout(@apple-orange, 90%);
              color: @apple-orange;
              
              .anticon {
                animation: spin 1.2s infinite linear;
              }
            }
            
            .card-value {
              color: @apple-orange;
            }
          }
          
          &.failed {
            .card-icon {
              background-color: fadeout(@apple-red, 90%);
              color: @apple-red;
            }
            
            .card-value {
              color: @apple-red;
            }
          }
        }
      }
    }
    
    // 服务列表样式
    .service-list {
      background-color: @apple-white;
      border-radius: 12px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
      overflow: hidden;
      
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
    }
    
    // 状态标签样式
    .status-tag {
      display: inline-flex;
      align-items: center;
      padding: 4px 10px;
      border-radius: 12px;
      font-size: 13px;
      font-weight: 500;
      
      .anticon {
        margin-right: 6px;
        font-size: 14px;
      }
      
      &.installing {
        background-color: fadeout(@apple-blue, 90%);
        color: @apple-blue;
        
        .anticon {
          animation: spin 1.2s infinite linear;
        }
      }
      
      &.success {
        background-color: fadeout(@apple-green, 90%);
        color: @apple-green;
      }
      
      &.failed {
        background-color: fadeout(@apple-red, 90%);
        color: @apple-red;
      }
      
      &.waiting {
        background-color: fadeout(@apple-gray, 90%);
        color: @apple-gray;
      }
    }
    
    // 进度条样式
    :deep(.apple-progress) {
      .ant-progress-inner {
        background-color: @apple-gray-light;
      }
      
      .ant-progress-bg {
        transition: all 0.3s;
      }
      
      .ant-progress-text {
        color: @apple-black;
        font-weight: 500;
      }
    }
    
    // 按钮样式
    .apple-button {
      margin-right: 8px;
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
      
      &.retry {
        background-color: fadeout(@apple-blue, 90%);
        color: @apple-blue;
        border: 1px solid fadeout(@apple-blue, 70%);
        
        &:hover {
          background-color: fadeout(@apple-blue, 80%);
        }
      }
      
      &.view-log {
        background-color: fadeout(@apple-gray, 90%);
        color: @apple-black;
        border: 1px solid fadeout(@apple-gray, 70%);
        
        &:hover {
          background-color: fadeout(@apple-gray, 80%);
        }
      }
    }
  }
  
  // 日志弹窗样式
  :deep(.apple-modal) {
    .ant-modal-content {
      border-radius: 12px;
      overflow: hidden;
    }
    
    .ant-modal-header {
      background-color: @apple-gray-light;
      border-bottom: none;
      padding: 16px 24px;
      
      .ant-modal-title {
        .apple-font();
        font-weight: 600;
        color: @apple-black;
      }
    }
    
    .ant-modal-body {
      padding: 24px;
      
      .log-content {
        max-height: 400px;
        overflow-y: auto;
        background-color: #f8f8f8;
        border-radius: 8px;
        padding: 16px;
        
        pre {
          margin: 0;
          white-space: pre-wrap;
          word-wrap: break-word;
          font-family: monospace;
          font-size: 13px;
          line-height: 1.5;
          color: @apple-black;
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

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style> 