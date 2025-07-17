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


 * @describe: step5-分配服务Master角色 
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-08-15 14:07:09
 * @FilePath: \ddh-ui\src\components\steps\step5.vue
-->
<template>
  <a-spin :spinning="loading" class="apple-spin-container">
    <div class="steps5 steps apple-style-container">
      <div class="steps-header">
        <div class="steps-title">
          <span>分配服务Master角色</span>
        </div>
        <div class="steps-subtitle">
          <span>请为每个服务的Master角色分配合适的主机</span>
        </div>
      </div>
      
      <div class="content-container">
        <CommonTemplate 
          ref="commonTemplateRef" 
          :steps4Data="steps4Data" 
          :templateData="templateData"
          class="apple-template" 
        />
      </div>
    </div>
  </a-spin>
</template>
<script>
import CommonTemplate from "@/components/commonTemplate/index";

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
      saveData: [],
      hostList: [],
    };
  },
  methods: {
    // 去除字符串里面的数字
    deleteNum(str, key) {
      let reg = /[0-9]+/g;
      let str1 = str.replace(reg, "");
      let str2 = str1.replace(key, "");
      return str2;
    },
    handleSubmit(callback) {
      this.$refs.commonTemplateRef.form.validateFields(async (err, values) => {
        if (!err) {
          // 处理表单数据 将相同的key处理成数组
          let formData = {};
          let saveParam = [];
          for (var k in values) {
            const key = this.deleteNum(k, "multipleSelect");
            if (k.includes("multipleSelect")) {
              if (Object.prototype.hasOwnProperty.call(formData, key)) {
                formData[`${key}`].push(values[k]);
              } else {
                formData[`${key}`] = [values[k]];
              }
            } else {
              if (
                  Object.prototype.toString.call(values[k]) === "[object Array]"
              ) {
                formData[`${k}`] = values[k];
              } else {
                formData[`${k}`] = [values[k]];
              }
            }
          }
          for (var label in formData) {
            saveParam.push({
              serviceRole: label,
              hosts: formData[label],
            });
            this.templateData.forEach((item) => {
              if (item.label === label) {
                item.value = formData[label];
              }
            });
          }
          // 等待网络请求结束
          let res = await this.$axiosJsonPost(
              global.API.saveServiceRoleHostMapping + `/${this.clusterId}`,
              saveParam
          );
          // 网络请求结束后才执行下边的语句  如果传入的callback方法为空或者没传内容也不会去执行，这样也不会影响此方法在别处的调用
          if (callback) {
            callback(res);
          }
        } else {
          if (callback) {
            callback({ code: 0 });
          }
        }
      });
    },
    getServiceRoleList() {
      const self = this;
      const params = {
        clusterId: this.clusterId,
        serviceIds: this.steps4Data.serviceIds.join(",") || "",
        serviceRoleType: 1, // 传1查的是Master角色
      };
      this.$axiosPost(global.API.getServiceRoleList, params).then((res) => {
        self.templateData = self.handlerData(res.data);
        self.loading = false;
      });
    },
    getAllHost() {
      this.loading = true;
      const params = {
        clusterId: this.clusterId,
      };
      this.$axiosPost(global.API.getAllHost, params).then((res) => {
        let arr = [];
        res.data.map((item) => {
          arr.push(item.hostname);
        });
        this.hostList = arr;
        this.getServiceRoleList();
      });
    },
    handlerData(data) {
      let arr = [];
      data.map((item) => {
        arr.push({
          label: item.serviceRoleName,
          name: item.serviceRoleName,
          value: item.hosts ? item.hosts : this.hostList.length > 1 ? this.hostList[0] : undefined,
          defaultValue: item.hosts ? item.hosts : this.hostList.length > 1 ? this.hostList[0] : undefined,
          selectValue: this.hostList,
          type: item.cardinality === "1" ? "select" : "multipleSelect",
          isHidden: false,
          required: item.serviceRoleType === "master",
        });
      });
      return arr;
    },
  },
  mounted() {
    this.getAllHost();
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

.apple-spin-container {
  min-height: 600px;
  
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

.apple-style-container {
  font-family: "SF Pro Display", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen, Ubuntu, Cantarell, "Open Sans", sans-serif;
  color: @apple-text;
  margin: 0;
  padding: 0;
  background-color: @apple-white;
}

.steps-header {
  padding: 0 0 24px 0;
  border-bottom: 1px solid @apple-border;
  margin-bottom: 24px;
  
  .steps-title {
    font-size: 24px;
    font-weight: 600;
    margin-bottom: 8px;
    color: @apple-text;
  }
  
  .steps-subtitle {
    font-size: 16px;
    color: @apple-gray-500;
  }
}

.content-container {
  background-color: @apple-white;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  padding: 24px;
  
  :deep(.apple-template) {
    // 表单项标签样式
    .ant-form-item-label label {
      color: @apple-text;
      font-weight: 500;
    }
    
    // 表单项控件样式
    .ant-form-item-control {
      // 选择器样式
      .ant-select {
        .ant-select-selection {
          border-radius: 8px;
          border-color: @apple-gray-300;
          transition: all 0.3s;
          
          &:hover {
            border-color: @apple-blue;
          }
          
          .ant-select-selection__rendered {
            margin-left: 12px;
          }
        }
        
        &.ant-select-focused .ant-select-selection,
        .ant-select-selection:focus,
        .ant-select-selection:active {
          border-color: @apple-blue;
          box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
        }
      }
      
      // 多选选择器样式
      .ant-select-selection--multiple {
        .ant-select-selection__choice {
          background-color: @apple-gray-100;
          border-color: @apple-gray-300;
          border-radius: 4px;
          margin-top: 6px;
          margin-bottom: 6px;
          
          .ant-select-selection__choice__content {
            margin-right: 6px;
          }
          
          .ant-select-selection__choice__remove {
            color: @apple-gray-500;
            
            &:hover {
              color: @apple-text;
            }
          }
        }
      }
      
      // 下拉菜单样式
      :deep(.ant-select-dropdown) {
        border-radius: 8px;
        box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
        
        .ant-select-dropdown-menu-item {
          padding: 10px 12px;
          transition: all 0.2s;
          
          &:hover {
            background-color: @apple-gray-100;
          }
          
          &-selected {
            color: @apple-blue;
            font-weight: 500;
            background-color: @apple-blue-light;
          }
        }
      }
    }
    
    // 表单项间距
    .ant-form-item {
      margin-bottom: 24px;
    }
  }
}
</style>