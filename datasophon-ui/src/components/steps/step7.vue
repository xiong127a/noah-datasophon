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
  <div class="steps7 steps apple-style-container">
    <div class="steps-header">
      <div class="header-content">
        <div class="steps-title">
          <span>服务配置</span>
        </div>
        <div class="steps-subtitle">
          <span>配置各个服务组件的运行参数</span>
        </div>
      </div>
      <a-button class="apple-button save-button" type="primary" @click="handleSubmit">
        <a-icon type="save" />
        保存配置
      </a-button>
    </div>
    
    <div class="content-container">
      <a-spin :spinning="loading" class="apple-spin">
        <a-tabs 
          v-model="serviceNameKey" 
          @change="callback" 
          class="apple-tabs"
        >
          <a-tab-pane 
            v-for="item in SERVICENAMES" 
            :key="item" 
            :tab="item" 
            :forceRender="true"
          />
        </a-tabs>
        
        <div 
          v-for="item in SERVICENAMES" 
          :key="item"
          :class="[
            'service-config-container',
            serviceNameKey === item ? 'active-container' : 'inactive-container',
            item + 'warp'
          ]"
        >
          <CommonTemplate 
            :ref="'CommonTemplateRef'+item" 
            :steps4Data="steps4Data" 
            :templateData="templateProps(item)"
            class="apple-template" 
          />
        </div>
      </a-spin>
    </div>
  </div>
</template>
<script>
import CommonTemplate from "@/components/commonTemplate/index";
import { mapActions, mapState } from "vuex";
import { de } from "date-fns/locale";

export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  components: { CommonTemplate },
  props: {
    steps4Data: Object,
  },
  data() {
    return {
      loading: false,
      templateData: [],
      // "ZOOKEEPER": [], "HDFS": [], "YARN": []
      templateObj: {},
      saveData: [],
      hostList: [],
      serviceNameKey: "",
      SERVICENAMES: [],
      selectKeys: [],
      // serviceContainerHeight: 0,
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
    this.getServiceConfigOption();
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
    .steps-title {
      font-size: 24px;
      font-weight: 600;
      margin-bottom: 8px;
      color: @apple-text;
      background: linear-gradient(120deg, @apple-text, #505050);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    
    .steps-subtitle {
      font-size: 16px;
      color: @apple-gray-500;
    }
  }
  
  .apple-button {
    height: 40px;
    padding: 0 20px;
    border-radius: 20px;
    font-size: 14px;
    font-weight: 500;
    display: flex;
    align-items: center;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    
    &.save-button {
      background: @apple-blue;
      border: none;
      color: white;
      box-shadow: 0 2px 6px rgba(0, 113, 227, 0.2);
      
      &:hover {
        background: darken(@apple-blue, 5%);
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(0, 113, 227, 0.3);
      }
      
      &:active {
        transform: translateY(0);
      }
      
      .anticon {
        margin-right: 6px;
        font-size: 14px;
      }
    }
  }
}

.content-container {
  position: relative;
  margin-top: 20px;
  
  .apple-spin {
    :deep(.ant-spin-dot) {
      .ant-spin-dot-item {
        background-color: @apple-blue;
      }
    }
    
    :deep(.ant-spin-text) {
      color: @apple-blue;
    }
  }
  
  .apple-tabs {
    :deep(.ant-tabs-bar) {
      border-bottom: none;
      margin: 0 0 24px 0;
      
      .ant-tabs-nav-container {
        font-size: 15px;
      }
      
      .ant-tabs-tab {
        padding: 12px 20px;
        margin: 0 8px 0 0;
        color: @apple-gray-500;
        font-weight: 500;
        transition: all 0.3s;
        border-radius: 8px;
        
        &:hover {
          color: @apple-blue;
        }
        
        &.ant-tabs-tab-active {
          color: @apple-blue;
          font-weight: 600;
          background: @apple-blue-light;
        }
      }
      
      .ant-tabs-ink-bar {
        display: none;
      }
    }
  }
  
  .service-config-container {
    background: @apple-white;
    border-radius: 12px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
    padding: 24px;
    margin-bottom: 24px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    
    &.active-container {
      opacity: 1;
      transform: translateY(0);
    }
    
    &.inactive-container {
      opacity: 0;
      transform: translateY(20px);
      pointer-events: none;
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
    }
    
    .apple-template {
      :deep(.ant-form-item) {
        margin-bottom: 24px;
        
        .ant-form-item-label {
          line-height: 1.5;
          
          label {
            color: @apple-text;
            font-weight: 500;
            font-size: 14px;
          }
        }
        
        .ant-form-item-control {
          line-height: 1.5;
          
          .ant-input {
            border-radius: 8px;
            border-color: @apple-gray-300;
            transition: all 0.3s;
            padding: 8px 12px;
            
            &:hover {
              border-color: @apple-blue;
            }
            
            &:focus {
              border-color: @apple-blue;
              box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
            }
          }
          
          .ant-select {
            .ant-select-selection {
              border-radius: 8px;
              border-color: @apple-gray-300;
              transition: all 0.3s;
              
              &:hover {
                border-color: @apple-blue;
              }
            }
            
            &.ant-select-focused .ant-select-selection {
              border-color: @apple-blue;
              box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
            }
          }
          
          .ant-checkbox-wrapper {
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
      }
    }
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

@keyframes fadeOut {
  from {
    opacity: 1;
    transform: translateY(0);
  }
  to {
    opacity: 0;
    transform: translateY(20px);
  }
}
</style> 