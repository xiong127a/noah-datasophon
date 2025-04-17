<!--
 * @describe: 连接信息面板 - 统一管理服务连接信息展示
-->
<template>
  <div class="connection-info-panel">
    <!-- 加载中状态 -->
    <div v-if="loading" class="loading-container">
      <a-spin size="large" />
    </div>
    
    <!-- 无数据状态 -->
    <a-empty v-else-if="!connectionInfo || isEmptyObject(connectionInfo)" description="暂无连接信息" />
    
    <!-- 有数据状态 -->
    <template v-else>
      <!-- 标签页导航 -->
      <a-tabs
        v-model="activeKey"
        :tabBarStyle="tabBarStyle"
        animated
      >
        <a-tab-pane key="basic" tab="基本信息">
          <div class="tab-content">
            <basic-info
              v-if="activeKey === 'basic'"
              :groupedInfo="groupedInfoData"
              :importantKeys="importantKeys"
            />
          </div>
        </a-tab-pane>
        
        <a-tab-pane key="java" tab="Java 代码" v-if="javaCodeExample">
          <div class="tab-content">
            <code-block
              v-if="activeKey === 'java'"
              :code="javaCodeExample"
              language="java"
              :title="javaTitle"
              :fileName="javaFileName"
              :dependencies="javaDependencies"
              :dependenciesSummary="javaDependenciesSummary"
            />
          </div>
        </a-tab-pane>
        
        <a-tab-pane key="python" tab="Python 代码" v-if="pythonCodeExample">
          <div class="tab-content">
            <code-block
              v-if="activeKey === 'python'"
              :code="pythonCodeExample"
              language="python"
              :title="pythonTitle"
              :fileName="pythonFileName"
              :dependencies="pythonDependencies"
              :dependenciesSummary="pythonDependenciesSummary"
            />
          </div>
        </a-tab-pane>
        
        <a-tab-pane key="shell" tab="命令行" v-if="commandLines && commandLines.length">
          <div class="tab-content">
            <command-terminal
              v-if="activeKey === 'shell'"
              :commands="commandLines"
              :serviceName="serviceName"
              :title="commandTitle"
            />
          </div>
        </a-tab-pane>
      </a-tabs>
    </template>
  </div>
</template>

<script>
import BasicInfo from './BasicInfo';
import CodeBlock from './CodeBlock';
import CommandTerminal from './CommandTerminal';

export default {
  name: 'ConnectionInfoPanel',
  components: {
    BasicInfo,
    CodeBlock,
    CommandTerminal
  },
  props: {
    // 服务ID
    serviceId: {
      type: String,
      required: true
    },
    // 服务名称
    serviceName: {
      type: String,
      default: ''
    },
    // 连接信息（可选，如果不提供则通过API获取）
    connectionInfo: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      loading: false,
      activeKey: 'basic',
      localConnectionInfo: null,
      tabBarStyle: {
        marginBottom: '24px',
        borderBottom: 'none',
        paddingBottom: '8px'
      }
    };
  },
  computed: {
    // 处理后的连接信息，优先使用props中的，如果没有则使用本地获取的
    processedConnectionInfo() {
      return this.connectionInfo || this.localConnectionInfo || {};
    },
    
    // 分组信息数据，供BasicInfo组件使用
    groupedInfoData() {
      const info = this.processedConnectionInfo;
      if (!info || this.isEmptyObject(info)) {
        return null;
      }
      
      // 转换为分组信息对象 - 从InfoItem列表构建Map结构
      const buildMapFromItems = (itemsList) => {
        if (!itemsList || !Array.isArray(itemsList)) return {};
        const result = {};
        itemsList.forEach(item => {
          if (item && item.key && item.displayName) {
            result[item.key] = {
              value: item.value,
              displayName: item.displayName
            };
          }
        });
        return result;
      };
      
      return {
        basicInfo: buildMapFromItems(info.basicInfoItems),
        securityInfo: buildMapFromItems(info.securityInfoItems),
        connectInfo: buildMapFromItems(info.connectInfoItems)
      };
    },
    
    // 重要信息键名列表
    importantKeys() {
      const info = this.processedConnectionInfo;
      return (info && info.importantKeys) || [];
    },
    
    // Java代码示例
    javaCodeExample() {
      const info = this.processedConnectionInfo;
      return (info && info.javaCode) || '';
    },
    
    // Java代码标题
    javaTitle() {
      const info = this.processedConnectionInfo;
      return (info && info.javaTitle) || 'Java 连接示例';
    },
    
    // Java代码文件名
    javaFileName() {
      const info = this.processedConnectionInfo;
      return (info && info.javaFileName) || 'Example.java';
    },
    
    // Python代码示例
    pythonCodeExample() {
      const info = this.processedConnectionInfo;
      return (info && info.pythonCode) || '';
    },
    
    // Python代码标题
    pythonTitle() {
      const info = this.processedConnectionInfo;
      return (info && info.pythonTitle) || 'Python 连接示例';
    },
    
    // Python代码文件名
    pythonFileName() {
      const info = this.processedConnectionInfo;
      return (info && info.pythonFileName) || 'example.py';
    },
    
    // Java依赖信息
    javaDependencies() {
      const info = this.processedConnectionInfo;
      return (info && info.javaDependencies) || '';
    },
    
    // Java依赖摘要
    javaDependenciesSummary() {
      const info = this.processedConnectionInfo;
      return (info && info.javaDependenciesSummary) || '';
    },
    
    // Python依赖信息
    pythonDependencies() {
      const info = this.processedConnectionInfo;
      return (info && info.pythonDependencies) || '';
    },
    
    // Python依赖摘要
    pythonDependenciesSummary() {
      const info = this.processedConnectionInfo;
      return (info && info.pythonDependenciesSummary) || '';
    },
    
    // 命令行示例
    commandLines() {
      const info = this.processedConnectionInfo;
      return (info && info.commandLines) || [];
    },
    
    // 命令行标题
    commandTitle() {
      const info = this.processedConnectionInfo;
      return (info && info.commandTitle) || '命令行示例';
    }
  },
  watch: {
    // 监听serviceId变化，重新获取连接信息
    serviceId: {
      immediate: true,
      handler(newValue, oldValue) {
        if (newValue && !this.connectionInfo) {
          this.fetchConnectionInfo();
        } else if (newValue !== oldValue) {
          // 如果serviceId变化，强制重新加载数据
          this.resetConnectionInfo();
          this.fetchConnectionInfo();
        }
      }
    },
    // 监听props连接信息变化
    connectionInfo(newValue) {
      if (newValue) {
        // 清空本地连接信息，完全使用prop传入的新值
        this.localConnectionInfo = null;
        // 重置为基本信息标签页
        this.activeKey = 'basic';
      }
    }
  },
  methods: {
    // 重置连接信息
    resetConnectionInfo() {
      this.localConnectionInfo = null;
      this.activeKey = 'basic';
    },
    
    // 获取服务连接信息
    async fetchConnectionInfo() {
      if (!this.serviceId) return;
      
      // 清空旧数据，避免显示过时信息
      this.localConnectionInfo = null;
      this.loading = true;
      try {
        const response = await this.$axiosPost(global.API.getConnectionInfo, {
          serviceInstanceId: this.serviceId
        });
        if (response && response.code === 200) {
          this.localConnectionInfo = response.data;
        } else {
          this.localConnectionInfo = null;
        }
      } catch (error) {
        console.error('获取连接信息失败:', error);
        this.$message.error('获取连接信息失败');
        this.localConnectionInfo = null;
      } finally {
        this.loading = false;
      }
    },
    
    // 判断对象是否为空
    isEmptyObject(obj) {
      return obj && typeof obj === 'object' && Object.keys(obj).length === 0;
    }
  }
};
</script>

<style lang="less" scoped>
.connection-info-panel {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  padding: 28px;
  min-height: 300px;
  position: relative;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", Arial, sans-serif;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.tab-content {
  padding: 4px;
  animation: fadeIn 0.35s cubic-bezier(0.28, 0.8, 0.35, 1);
}

/* 自定义Tabs样式，参考苹果设计 */
:deep(.ant-tabs-nav) {
  &::before {
    display: none !important;
  }
  
  .ant-tabs-tab {
    font-size: 15px;
    padding: 10px 18px;
    margin: 0 16px 0 0;
    transition: all 0.25s cubic-bezier(0.28, 0.8, 0.35, 1);
    border-radius: 18px;
    border: none;
    color: #1d1d1f;
    opacity: 0.75;
    background: transparent;
    position: relative;
    
    &:hover {
      color: #06c;
      opacity: 0.9;
      background: rgba(0, 0, 0, 0.025);
    }
    
    &.ant-tabs-tab-active {
      .ant-tabs-tab-btn {
        color: #06c;
        font-weight: 500;
        text-shadow: 0 0 0 #06c;
      }
      opacity: 1;
      background: rgba(0, 102, 204, 0.06);
    }
  }
  
  .ant-tabs-ink-bar {
    display: none !important;
  }
}

/* 空状态样式 */
:deep(.ant-empty) {
  padding: 64px 0;
  
  .ant-empty-image {
    height: 100px;
    margin-bottom: 20px;
    opacity: 0.8;
  }
  
  .ant-empty-description {
    color: #86868b;
    font-size: 15px;
    letter-spacing: -0.01em;
  }
}

/* 加载中状态样式 */
.loading-container {
  .ant-spin {
    .ant-spin-dot-item {
      background-color: #06c;
    }
  }
}

/* 淡入动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式样式 */
@media screen and (max-width: 768px) {
  .connection-info-panel {
    padding: 20px;
    border-radius: 12px;
  }
  
  :deep(.ant-tabs-nav) {
    .ant-tabs-tab {
      padding: 8px 12px;
      margin: 0 8px 0 0;
      font-size: 14px;
    }
  }
}
</style> 