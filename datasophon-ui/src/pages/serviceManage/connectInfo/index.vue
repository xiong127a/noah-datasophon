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


 * @describe: 连接信息主入口组件
-->
<template>
  <div class="connect-info-container">
    <!-- 加载状态 -->
    <div class="loading-container" v-if="loading">
      <a-spin>
        <div class="loading-content">
          <a-icon type="loading" class="loading-icon" />
          <div class="loading-text">数据加载中...</div>
        </div>
      </a-spin>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <a-result status="error" title="加载失败" :sub-title="errorMessage">
        <template #extra>
          <a-button type="primary" @click="retryLoading">
            重试
          </a-button>
        </template>
      </a-result>
    </div>
    
    <!-- 动态加载对应的服务连接信息组件 -->
    <component 
      v-else-if="currentServiceComponent" 
      :is="currentServiceComponent"
      :service-id="serviceId"
      :service-name="serviceName"
      :connection-info="connectionInfo"
      @loading-change="handleLoadingChange"
    />
    
    <!-- 服务未支持或组件不存在 -->
    <a-result 
      v-else 
      status="info" 
      title="未配置连接信息" 
      sub-title="该服务暂未配置连接信息展示页面"
    />
  </div>
</template>

<script>
// 动态导入services目录下的所有Vue组件
// 使用webpack的require.context功能
const servicesContext = require.context('./services', false, /\.vue$/);

// 创建组件映射表
const SERVICE_COMPONENT_MAP = {};
const dynamicComponents = {};

// 遍历所有匹配的文件
servicesContext.keys().forEach(fileName => {
  // 从文件名中提取服务类型名称 (例如 './Hive.vue' -> 'HIVE')
  const serviceType = fileName.replace(/^\.\/(.*?)\.vue$/, '$1').toUpperCase();
  // 导入组件
  const component = servicesContext(fileName).default;
  
  // 添加到映射表
  SERVICE_COMPONENT_MAP[serviceType] = component;
  // 添加到动态组件对象
  dynamicComponents[serviceType + 'ConnectInfo'] = component;
});

// 输出发现的服务组件列表，用于调试
console.log('支持连接信息的服务类型:', Object.keys(SERVICE_COMPONENT_MAP));

export default {
  name: "ConnectInfoContainer",
  // 动态注册所有发现的组件
  components: {
    ...dynamicComponents
  },
  props: {
    serviceId: {
      type: [String, Number],
      required: true
    },
    // 可选：服务名称
    serviceName: {
      type: String,
      default: ''
    },
    // 可选：服务类型，用于直接匹配组件
    serviceType: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      loading: true,
      error: false,
      errorMessage: '',
      serviceData: null,
      connectionInfo: null,
      // 存储支持的服务类型列表
      supportedServiceTypes: Object.keys(SERVICE_COMPONENT_MAP)
    };
  },
  computed: {
    // 检查当前服务类型是否支持
    isServiceSupported() {
      const serviceType = this.getServiceType();
      return serviceType && this.supportedServiceTypes.includes(serviceType.toUpperCase());
    },
    
    // 根据服务类型确定要加载的组件
    currentServiceComponent() {
      const serviceType = this.getServiceType();
      if (!serviceType) return null;
      
      // 返回对应的组件，如果不存在则返回null
      return SERVICE_COMPONENT_MAP[serviceType.toUpperCase()] || null;
    }
  },
  watch: {
    serviceId: {
      immediate: true,
      handler(newVal) {
        if (newVal) {
          this.getConnectionInfo();
        }
      }
    }
  },
  methods: {
    // 获取服务类型 - 直接从服务名称获取，无需额外调用API
    getServiceType() {
      if (this.serviceType) {
        // 如果直接提供了服务类型，使用它
        return this.serviceType;
      } else if (this.serviceName) {
        // 从服务名称中提取类型
        return this.serviceName;
      } else if (this.serviceData && this.serviceData.type) {
        // 从服务数据中提取类型
        return this.serviceData.type;
      }
      
      // 如果没有服务类型信息，返回null
      return null;
    },
    
    // 获取连接信息 - 直接使用原始API
    getConnectionInfo() {
      if (!this.serviceId) {
        console.error("缺少serviceId参数，无法获取连接信息");
        this.$message.warning("服务ID未设置，无法获取连接信息");
        return;
      }
      
      console.log("开始获取连接信息，serviceId:", this.serviceId, "服务名称:", this.serviceName);
      this.loading = true;
      this.error = false;
      
      this.$axiosPost(global.API.getConnectionInfo, {
        serviceInstanceId: this.serviceId
      })
        .then((res) => {
          console.log("连接信息响应:", res);
          
          if (res.code === 200) {
            this.connectionInfo = res.data || {};
            
            // 打印收到的数据，用于调试
            console.log("收到的连接信息数据:", JSON.stringify(this.connectionInfo));
            
            // 检查是否包含commandLines
            if (this.connectionInfo && this.connectionInfo.commandLines) {
              console.log("命令行信息:", this.connectionInfo.commandLines.length, "条");
            } else {
              console.warn("未找到命令行信息");
            }
            
            // 获取到数据后设置服务类型（如果还没有）
            if (this.connectionInfo) {
              // 尝试从连接信息中提取服务类型
              const serviceType = this.connectionInfo.serviceType || 
                                 this.connectionInfo.type || 
                                 (this.connectionInfo.basicInfo && this.connectionInfo.basicInfo.serviceType) ||
                                 // 如果返回数据中有明显的Hive特征，设置为HIVE
                                 (this.connectionInfo.jdbcUrl && this.connectionInfo.jdbcUrl.toLowerCase().includes('hive') ? 'HIVE' : null) ||
                                 (this.connectionInfo.beelineCommand ? 'HIVE' : null);
              
              if (serviceType) {
                this.serviceData = { type: serviceType };
              } else {
                // 服务类型仍未确定，尝试直接使用服务名称
                this.serviceData = { type: this.serviceName || 'HIVE' };
              }
            }
            
            // 输出更详细的调试信息
            console.log("当前服务类型:", this.getServiceType());
            console.log("支持的服务类型:", this.supportedServiceTypes);
            console.log("是否支持:", this.isServiceSupported);
            console.log("匹配的组件:", this.currentServiceComponent ? '有' : '无');
            
            // 检查服务类型是否支持
            if (this.isServiceSupported) {
              console.log("当前服务支持连接信息展示");
            } else {
              console.warn("当前服务不支持连接信息展示:", this.getServiceType());
            }
          } else {
            this.error = true;
            this.errorMessage = res.msg || "获取连接信息失败";
          }
        })
        .catch((error) => {
          console.error('获取连接信息错误:', error);
          this.error = true;
          this.errorMessage = error.message || '连接信息加载失败，请重试';
        })
        .finally(() => {
          this.loading = false;
        });
    },
    
    // 重试加载
    retryLoading() {
      this.getConnectionInfo();
    },
    
    // 处理子组件的加载状态变化
    handleLoadingChange(isLoading) {
      this.loading = isLoading;
    },

    // 为了兼容旧版本callback方法，保留getConnectionInfo方法的命名
    fetchServiceInfo() {
      console.log('触发fetchServiceInfo方法，调用getConnectionInfo');
      this.getConnectionInfo();
    }
  }
};
</script>

<style lang="less" scoped>
.connect-info-container {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  min-height: 300px;
  position: relative;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  
  .loading-content {
    text-align: center;
    
    .loading-icon {
      font-size: 28px;
      margin-bottom: 16px;
    }
    
    .loading-text {
      color: rgba(0, 0, 0, 0.45);
    }
  }
}

.error-container {
  min-height: 300px;
  display: flex;
  justify-content: center;
  align-items: center;
}
</style> 