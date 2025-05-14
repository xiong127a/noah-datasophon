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


 * @describe: 服务配置
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-10-27 11:01:48
 * @FilePath: \ddh-ui\src\pages\serviceManage\setting.vue
-->
<template>
  <div class="service-setting steps">
    <!-- 添加Tab页签 -->
    <a-tabs :activeKey="activeTabKey" @change="handleTabChange">
      <a-tab-pane key="service-config" tab="服务配置" :forceRender="true">
        <ServiceConfig 
          :steps4Data="steps4Data" 
          :serviceId="serviceId" 
          :serviceName="serviceName"
        />
      </a-tab-pane>
      
      <a-tab-pane key="k8s-config" tab="Kubernetes 仪表盘" :forceRender="true">
        <K8sConfig :serviceId="serviceId" :serviceName="serviceName" :clusterId="clusterId"/>
      </a-tab-pane>
      
      <a-tab-pane key="config-download" tab="配置导出" :forceRender="true">
        <ConfigDownload :serviceId="serviceId" :serviceName="serviceName" />
      </a-tab-pane>
    </a-tabs>

  </div>
</template>
<script>
import {mapActions, mapState} from "vuex";
import {getServiceName} from "@/utils/util";
import ConfigDownload from "./config/components/ConfigDownload.vue";
import KubernetesDashboard from "./config/kubernetes/dashboard/KubernetesDashboard.vue";
import ServiceConfig from "./components/ServiceConfig.vue";

export default {
  components: {ServiceConfig, ConfigDownload, K8sConfig: KubernetesDashboard},
  props: {
    steps4Data: Object,
  },
  data() {
    return {
      clusterId: Number(localStorage.getItem("clusterId") || -1),
      activeTabKey: 'service-config',
      serviceId: '',
      serviceName: ''
    };
  },
  computed: {
    ...mapState({
      steps: (state) => state.steps, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
    }),
  },
  methods: {
    ...mapActions("steps", ["setCommandType", "setCommandIds"]),
    handleTabChange(key) {
      this.activeTabKey = key;
      
      // 确保serviceName有值
      if (!this.serviceName && this.$route.params.serviceId) {
        // 从菜单数据中获取服务名称
        const serviceId = this.$route.params.serviceId;
        const menuData = JSON.parse(localStorage.getItem('menuData')) || [];
        const arr = menuData.filter(item => item.path === 'service-manage');
        if (arr.length > 0) {
          arr[0].children.forEach(item => {
            if (item.meta.params.serviceId == serviceId) {
              this.serviceName = item.name;
            }
          });
        }
        
        // 如果还是没有找到，则使用默认值
        if (!this.serviceName) {
          this.serviceName = "未知服务";
          console.warn('无法获取服务名称，使用默认值');
        }
      }
    },
  },
  created() {
    // 从query和params中获取参数
    const queryParams = this.$route.query;
    const routeParams = this.$route.params;
    
    console.log('setting.vue创建, 查询参数:', queryParams);
    console.log('路由参数:', routeParams);
    
    // 优先使用query中的参数，如果没有则使用params中的
    const serviceInstanceId = queryParams.serviceInstanceId || routeParams.serviceId;
    const serviceName = queryParams.serviceName;
    const serviceType = queryParams.serviceType;
    
    console.log('合并后serviceInstanceId:', serviceInstanceId);
    console.log('serviceName:', serviceName);
    console.log('serviceType:', serviceType);
    
    this.serviceId = serviceInstanceId;
    
    // 设置serviceName
    if (serviceName) {
      this.serviceName = serviceName;
    } else if (serviceType) {
      this.serviceName = getServiceName(serviceType);
    } else {
      // 从菜单数据中获取服务名称
      const menuData = JSON.parse(localStorage.getItem('menuData')) || [];
      const arr = menuData.filter(item => item.path === 'service-manage');
      if (arr.length > 0 && arr[0].children) {
        arr[0].children.forEach(item => {
          if (item.meta && item.meta.params && item.meta.params.serviceId == serviceInstanceId) {
            this.serviceName = item.name;
          }
        });
      }
      
      // 如果还是没有找到，使用默认值
      if (!this.serviceName) {
        this.serviceName = "未知服务";
        console.warn('无法获取服务名称，使用默认值');
      }
    }
    
    console.log('设置后的serviceId:', this.serviceId);
    console.log('设置后的serviceName:', this.serviceName);
  },
};
</script>
<style lang="less" scoped>
.service-setting {
  /deep/ .ant-spin-container {
    position: relative;
  }
}
</style>
