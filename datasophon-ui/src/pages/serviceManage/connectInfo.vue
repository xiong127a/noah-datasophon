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


 * @describe: 连接信息组件
-->
<template>
  <div class="connection-container">
    <!-- 加载状态 -->
    <div class="loading-container" v-if="loading">
      <a-spin>
        <a-icon slot="indicator" type="loading" class="loading-icon" />
        <div class="loading-text">正在获取连接信息...</div>
      </a-spin>
    </div>

    <!-- 数据为空状态 -->
    <div class="empty-container" v-else-if="!connectionInfo || isEmpty">
      <a-empty description="暂无连接信息">
        <a-button type="primary" @click="refreshData">刷新</a-button>
      </a-empty>
    </div>

    <!-- 连接信息内容 -->
    <div class="connection-content" v-else>
      <!-- 标题和刷新按钮 -->
      <div class="connection-header">
        <div class="title">连接信息</div>
      </div>

      <!-- 标签页切换 -->
      <div class="connection-tabs">
        <div 
          v-for="(tab, index) in tabs" 
          :key="index" 
          :class="['tab-item', { active: activeTab === index }]"
          @click="activeTab = index"
        >
          <a-icon :type="tab.icon" class="tab-icon" />
          <span class="tab-text">{{ tab.title }}</span>
        </div>
      </div>

      <!-- 基本信息Tab -->
      <div class="tab-content" v-if="activeTab === 0">
        <div class="info-card">
          <div class="info-list">
            <div 
              v-for="item in basicInfoArray" 
              :key="item.label" 
              class="info-item"
            >
              <div class="info-label">{{ item.label }}</div>
              <div class="info-value">
                {{ item.value }}
                <a-icon
                  type="copy"
                  class="copy-icon"
                  @click="copyText(item.value)"
                  title="复制"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- JDBC URLs Tab -->
      <div class="tab-content" v-if="activeTab === 1">
        <div class="info-card">
          <div class="jdbc-url-container">
            <div class="url-header">
              <span>JDBC URL</span>
              <a-icon
                type="copy"
                class="copy-icon-large"
                @click="copyText(connectionInfo.jdbcUrl)"
                title="复制"
              />
            </div>
            <div class="url-content">
              {{ connectionInfo.jdbcUrl }}
            </div>
          </div>
        </div>
      </div>

      <!-- Java示例代码Tab -->
      <div class="tab-content" v-if="activeTab === 2">
        <div class="info-card">
          <div class="code-container">
            <div class="code-header">
              <span>Java示例代码</span>
              <a-icon
                type="copy"
                class="copy-icon-large"
                @click="copyText(connectionInfo.javaCode)"
                title="复制全部代码"
              />
            </div>
            <pre class="code-content"><code>{{ connectionInfo.javaCode }}</code></pre>
          </div>
        </div>
      </div>

      <!-- Python示例代码Tab -->
      <div class="tab-content" v-if="activeTab === 3">
        <div class="info-card">
          <div class="code-container">
            <div class="code-header">
              <span>Python示例代码</span>
              <a-icon
                type="copy"
                class="copy-icon-large"
                @click="copyText(connectionInfo.pythonCode)"
                title="复制全部代码"
              />
            </div>
            <pre class="code-content"><code>{{ connectionInfo.pythonCode }}</code></pre>
          </div>
        </div>
      </div>

      <!-- 命令行Tab -->
      <div class="tab-content" v-if="activeTab === 4">
        <div class="info-card">
          <div class="cmd-container">
            <div class="cmd-header">
              <span>命令行</span>
              <a-icon
                type="copy"
                class="copy-icon-large"
                @click="copyText(connectionInfo.beelineCommand)"
                title="复制命令"
              />
            </div>
            <div class="cmd-content">
              <code>{{ connectionInfo.beelineCommand }}</code>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "ConnectInfo",
  props: {
    serviceId: {
      type: [String, Number],
      required: true
    }
  },
  data() {
    return {
      loading: false,
      refreshing: false,
      connectionInfo: null,
      activeTab: 0,
      tabs: [
        { title: '基本信息', icon: 'info-circle' },
        { title: 'JDBC', icon: 'api' },
        { title: 'Java', icon: 'code' },
        { title: 'Python', icon: 'code' },
        { title: '命令行', icon: 'console-sql' }
      ]
    };
  },
  computed: {
    isEmpty() {
      if (!this.connectionInfo) return true;
      
      // 检查是否所有关键字段都为空
      const { basicInfo, jdbcUrl, javaCode, pythonCode, beelineCommand } = this.connectionInfo;
      return (!basicInfo || Object.keys(basicInfo).length === 0) &&
             !jdbcUrl && !javaCode && !pythonCode && !beelineCommand;
    },
    basicInfoArray() {
      if (!this.connectionInfo || !this.connectionInfo.basicInfo) return [];
      
      // 将basicInfo对象转换为数组格式，适配列表展示
      return Object.entries(this.connectionInfo.basicInfo).map(([label, value]) => ({
        label,
        value
      }));
    }
  },
  created() {
    console.log("ConnectInfo组件被创建，serviceId:", this.serviceId);
  },
  mounted() {
    console.log("ConnectInfo组件已挂载，serviceId:", this.serviceId);
    this.getConnectionInfo();
  },
  methods: {
    getConnectionInfo() {
      const serviceInstanceId = this.serviceId;
      
      if (!serviceInstanceId) {
        console.error("缺少serviceId参数，无法获取连接信息");
        this.$message.warning("服务ID未设置，无法获取连接信息");
        return;
      }
      
      console.log("开始获取连接信息，serviceInstanceId:", serviceInstanceId);
      this.loading = true;
      
      this.$axiosPost(global.API.getConnectionInfo, {
        serviceInstanceId: serviceInstanceId
      })
        .then((res) => {
          console.log("连接信息响应:", res);
          
          if (res.code === 200) {
            this.connectionInfo = res.data || {};
            console.log("获取到的连接信息:", this.connectionInfo);
          } else {
            this.$message.error(res.msg || "获取连接信息失败");
            this.connectionInfo = null;
          }
        })
        .catch((err) => {
          console.error("获取连接信息失败:", err);
          this.$message.error("连接服务器失败");
          this.connectionInfo = null;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    refreshData() {
      this.refreshing = true;
      this.getConnectionInfo();
      
      // 添加短暂延迟让用户感知刷新操作
      setTimeout(() => {
        this.refreshing = false;
      }, 800);
    },
    copyText(text) {
      if (!text) return;
      
      const textarea = document.createElement("textarea");
      textarea.value = text;
      document.body.appendChild(textarea);
      textarea.select();
      
      try {
        document.execCommand("copy");
        this.$message.success("复制成功");
      } catch (err) {
        this.$message.error("复制失败");
      } finally {
        document.body.removeChild(textarea);
      }
    }
  },
  watch: {
    serviceId: {
      handler(newVal) {
        if (newVal) {
          console.log("serviceId变更为:", newVal);
          this.getConnectionInfo();
        }
      },
      immediate: true
    }
  }
};
</script>

<style lang="less" scoped>
.connection-container {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  position: relative;
  padding: 24px;
  min-height: 400px;
  transition: all 0.3s ease;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
  
  .loading-icon {
    font-size: 24px;
  }
  
  .loading-text {
    margin-top: 16px;
    color: #999;
    font-size: 14px;
  }
}

.empty-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.connection-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  
  .title {
    font-size: 20px;
    font-weight: 600;
    color: #1d1d1f;
  }
}

.connection-tabs {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 24px;
}

.tab-item {
  padding: 12px 16px;
  margin-right: 8px;
  cursor: pointer;
  color: #666;
  position: relative;
  font-size: 14px;
  transition: all 0.3s;
  white-space: nowrap;
  
  .tab-icon {
    margin-right: 6px;
  }
  
  &.active {
    color: #0071e3;
    font-weight: 500;
    
    &:after {
      content: "";
      position: absolute;
      bottom: -1px;
      left: 0;
      width: 100%;
      height: 2px;
      background: #0071e3;
      border-radius: 2px 2px 0 0;
    }
  }
  
  &:hover:not(.active) {
    color: #333;
  }
}

.info-card {
  background: #ffffff;
  border-radius: 12px;
  transition: all 0.3s ease-in-out;
  
  .info-list {
    .info-item {
      display: flex;
      padding: 14px 0;
      border-bottom: 1px solid #f5f5f7;
      
      &:last-child {
        border-bottom: none;
      }
      
      .info-label {
        flex: 0 0 160px;
        color: #86868b;
        font-size: 14px;
      }
      
      .info-value {
        flex: 1;
        color: #1d1d1f;
        font-size: 14px;
        display: flex;
        align-items: center;
        word-break: break-all;
        
        .copy-icon {
          margin-left: 8px;
          color: #0071e3;
          cursor: pointer;
          opacity: 0;
          transition: opacity 0.2s;
        }
      }
      
      &:hover {
        .copy-icon {
          opacity: 1;
        }
      }
    }
  }
}

.jdbc-url-container,
.code-container,
.cmd-container {
  border-radius: 12px;
  overflow: hidden;
  background: #f5f5f7;
  margin-bottom: 16px;
  
  .url-header,
  .code-header,
  .cmd-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: #f5f5f7;
    border-bottom: 1px solid #e5e5e5;
    font-weight: 500;
    color: #1d1d1f;
    
    .copy-icon-large {
      color: #0071e3;
      cursor: pointer;
      font-size: 16px;
      
      &:hover {
        color: #0077ed;
      }
    }
  }
  
  .url-content,
  .cmd-content {
    padding: 16px;
    background: #f9f9f9;
    white-space: pre-wrap;
    word-break: break-all;
    font-family: 'SFMono-Regular', Menlo, Monaco, Consolas, monospace;
    font-size: 13px;
    color: #333;
    line-height: 1.5;
  }
  
  code {
    font-family: 'SFMono-Regular', Menlo, Monaco, Consolas, monospace;
  }
}

.code-content {
  margin: 0;
  padding: 16px;
  background: #f9f9f9;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'SFMono-Regular', Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  color: #333;
  line-height: 1.5;
  overflow-x: auto;
  
  &::-webkit-scrollbar {
    height: 6px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: #ccc;
    border-radius: 3px;
  }
  
  &::-webkit-scrollbar-track {
    background: #f5f5f5;
  }
}

.copy-icon {
  cursor: pointer;
  transition: all 0.2s;
  
  &:hover {
    color: #0077ed;
  }
}
</style> 