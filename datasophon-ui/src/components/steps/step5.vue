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
  <div class="steps5 steps">
    <div class="hero-section">
      <h1 class="hero-title">分配主机</h1>
      <p class="hero-subtitle">为不同组件选择合适的主机，优化资源分配和集群性能</p>
    </div>
    
    <div class="mgt16 steps-body scroll-limit pdr30">
      <!-- 服务关系引用 -->
      <a-tabs v-if="activeKey.length > 0" v-model="tabActive">
        <a-tab-pane :tab="item.serviceName" v-for="item in activeKey" :key="item.serviceId">
          <div class="table-up-info" v-if="tabActive === item.serviceId">
            <CommonTemplate ref="commonTemplateRef" :steps4Data="steps4Data" :templateData="templateData" />
          </div>
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
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
      activeKey: [],
      tabActive: null,
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
// 添加苹果设计系统颜色和字体定义
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

.steps5 {
  margin: 0;
  max-width: 100%;
  background-color: @apple-white;
  overflow: hidden;
  animation: fadeIn 0.8s ease-out;
  
  .hero-section {
    text-align: center;
    margin-bottom: 3.5rem;
    
    .hero-title {
      .apple-font();
      font-size: 2.8rem;
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
      font-size: 1.4rem;
      line-height: 1.4;
      letter-spacing: 0;
      font-weight: 400;
      color: @apple-gray;
      margin: 0;
      max-width: 760px;
      margin: 0 auto;
    }
  }

  // 保留原有样式...
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>