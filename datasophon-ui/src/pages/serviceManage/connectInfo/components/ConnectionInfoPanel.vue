<!--
 * @describe: 连接信息面板 - 统一管理服务连接信息展示
-->
<template>
  <div class="connection-wrapper">
    <!-- 加载状态 -->
    <div class="loading-container" v-if="loading">
      <a-spin>
        <div class="loading-content">
          <a-icon type="loading" class="loading-icon" />
          <div class="loading-text">数据加载中...</div>
        </div>
      </a-spin>
    </div>

    <!-- 数据为空状态 -->
    <a-empty 
      v-else-if="!connectionInfo || isEmpty" 
      class="empty-container"
      description="暂无可用的连接信息"
    />

    <!-- 连接信息内容 -->
    <template v-else>
      <div class="connection-header">
        <h1 class="title">连接信息</h1>
        <a-tooltip title="刷新">
          <a-button 
            type="primary" 
            shape="circle" 
            icon="sync"
            :loading="refreshing"
            @click="refreshConnectionInfo"
          />
        </a-tooltip>
      </div>

      <!-- 标签页导航 -->
      <div class="segment-control">
        <div 
          v-for="(tab, index) in tabs" 
          :key="index"
          class="segment-item"
          :class="{ active: activeTab === index }"
          @click="activeTab = index"
        >
          {{ tab.title }}
        </div>
      </div>

      <!-- 容器内容区域 -->
      <div class="content-area">
        <!-- 基本信息 -->
        <div v-if="activeTab === 0" class="info-panel">
          <BasicInfo
            :info-items="basicInfoArray"
            :jdbc-items="jdbcUrlArray"
          />
        </div>

        <!-- Java 示例代码 -->
        <div v-else-if="activeTab === 1" class="info-panel">
          <CodeBlock
            :title="javaTitle"
            :file-name="javaFileName"
            :code="connectionInfo?.javaCode || ''"
            language="java"
            ref="javaCodeBlock"
          />
        </div>

        <!-- Python 示例代码 -->
        <div v-else-if="activeTab === 2" class="info-panel">
          <CodeBlock
            :title="pythonTitle"
            :file-name="pythonFileName"
            :code="connectionInfo?.pythonCode || ''"
            language="python"
            ref="pythonCodeBlock"
          />
        </div>

        <!-- 命令行 -->
        <div v-else-if="activeTab === 3" class="info-panel">
          <CommandTerminal 
            :title="commandTitle"
            :commands="commandLineArray"
            :host-name="connectionInfo?.hostName || getServiceHost()"
            :service-home="connectionInfo?.serviceHome || ''"
            :copy-tooltip="'复制所有命令'"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import BasicInfo from './BasicInfo';
import CodeBlock from './CodeBlock';
import CommandTerminal from './CommandTerminal';

export default {
  name: "ConnectionInfoPanel",
  components: {
    BasicInfo,
    CodeBlock,
    CommandTerminal
  },
  props: {
    // 服务实例ID
    serviceId: {
      type: [String, Number],
      required: true
    },
    // 服务名称
    serviceName: {
      type: String,
      default: ''
    },
    // 接收的连接信息对象
    connectionInfo: {
      type: Object,
      default: () => null
    },
    // 自定义标题
    titles: {
      type: Object,
      default: () => ({
        javaTitle: 'Java连接示例',
        pythonTitle: 'Python连接示例',
        commandTitle: '常用命令'
      })
    },
    // 自定义文件名
    fileNames: {
      type: Object,
      default: () => ({
        javaFileName: 'Example.java',
        pythonFileName: 'example.py'
      })
    }
  },
  data() {
    return {
      loading: false,
      refreshing: false,
      activeTab: 0,
      tabs: [
        { title: '基本信息' },
        { title: 'Java代码' },
        { title: 'Python代码' },
        { title: '命令行' }
      ]
    };
  },
  computed: {
    // 提取标题
    javaTitle() {
      return this.titles.javaTitle || 'Java连接示例';
    },
    pythonTitle() {
      return this.titles.pythonTitle || 'Python连接示例';
    },
    commandTitle() {
      return this.titles.commandTitle || '常用命令';
    },
    // 提取文件名
    javaFileName() {
      return this.fileNames.javaFileName || 'Example.java';
    },
    pythonFileName() {
      return this.fileNames.pythonFileName || 'example.py';
    },
    // 检查连接信息是否为空
    isEmpty() {
      if (!this.connectionInfo) return true;
      
      // 检查是否所有关键字段都为空
      const { basicInfo, jdbcUrl, javaCode, pythonCode, commandLines } = this.connectionInfo;
      return (!basicInfo || Object.keys(basicInfo).length === 0) &&
             !jdbcUrl && !javaCode && !pythonCode && (!commandLines || commandLines.length === 0);
    },
    // 处理基本信息数组
    basicInfoArray() {
      if (!this.connectionInfo) return [];
      
      // 优先使用后端提供的有序basicInfoList
      if (this.connectionInfo.basicInfoList && this.connectionInfo.basicInfoList.length > 0) {
        return this.connectionInfo.basicInfoList;
      }
      
      // 兼容处理：如果没有basicInfoList，则从basicInfo对象转换
      if (this.connectionInfo.basicInfo) {
        return Object.entries(this.connectionInfo.basicInfo).map(([label, value]) => ({
          label,
          value: value || '-'
        }));
      }
      
      return [];
    },
    // 处理JDBC URL数组
    jdbcUrlArray() {
      if (!this.connectionInfo) return [];
      
      // 优先使用后端提供的jdbcUrls数组
      if (this.connectionInfo.jdbcUrls && this.connectionInfo.jdbcUrls.length > 0) {
        return this.connectionInfo.jdbcUrls;
      }
      
      // 兼容处理：如果没有jdbcUrls，但有jdbcUrl字段
      if (this.connectionInfo.jdbcUrl) {
        return [{
          label: 'JDBC URL',
          value: this.connectionInfo.jdbcUrl
        }];
      }
      
      // Kafka特殊处理
      if (this.connectionInfo.kafkaUrl) {
        return [{
          label: 'Kafka Broker URL',
          value: this.connectionInfo.kafkaUrl
        }];
      }
      
      return [];
    },
    // 处理命令行数组
    commandLineArray() {
      if (!this.connectionInfo) return [];
      
      // 优先使用后端提供的commandLines数组
      if (this.connectionInfo.commandLines && this.connectionInfo.commandLines.length > 0) {
        return this.connectionInfo.commandLines;
      }
      
      // 兼容处理：如果没有commandLines，但有beelineCommand字段 (Hive专用)
      if (this.connectionInfo.beelineCommand) {
        return [{
          label: 'Beeline命令',
          value: this.connectionInfo.beelineCommand
        }];
      }
      
      return [];
    }
  },
  watch: {
    // 标签页切换时触发代码高亮
    activeTab: {
      handler(newVal) {
        this.$nextTick(() => {
          // 代码高亮由CodeBlock组件自行处理
        });
      }
    }
  },
  mounted() {
    // 如果没有connectionInfo，则获取连接信息
    if (!this.connectionInfo && this.serviceId) {
      this.getConnectionInfo();
    }
  },
  methods: {
    // 获取服务主机名 - 根据不同服务类型提取主机名
    getServiceHost() {
      if (!this.connectionInfo || !this.connectionInfo.basicInfo) return 'localhost';
      
      const serviceType = this.serviceName && this.serviceName.toUpperCase() || '';
      
      // 根据服务类型获取主机名
      if (serviceType === 'HIVE') {
        return this.getHiveHost();
      } else if (serviceType === 'KAFKA') {
        return this.getKafkaHost();
      }
      
      // 默认尝试获取任何主机名信息
      for (const key in this.connectionInfo.basicInfo) {
        if (key.includes('主节点') || key.includes('host') || key.includes('Host')) {
          const hostValue = this.connectionInfo.basicInfo[key];
          if (typeof hostValue === 'string' && hostValue.includes(':')) {
            return hostValue.split(':')[0];
          }
        }
      }
      
      return 'localhost';
    },
    
    // 获取Hive服务器主机名
    getHiveHost() {
      if (!this.connectionInfo || !this.connectionInfo.basicInfo) return 'localhost';
      
      // 尝试获取HiveServer2主节点
      const mainNode = this.connectionInfo.basicInfo['HiveServer2主节点'];
      if (mainNode) {
        // 提取主机名部分（去掉端口）
        const hostPart = mainNode.split(':')[0];
        return hostPart || 'localhost';
      }
      
      // 尝试从JDBC URL中获取主机名
      if (this.connectionInfo.jdbcUrl) {
        // eslint-disable-next-line no-useless-escape
        const match = this.connectionInfo.jdbcUrl.match(/jdbc:hive2:\/\/(.*?)[:\/]/);
        if (match && match[1]) {
          return match[1];
        }
      }
      
      return 'localhost';
    },
    
    // 获取Kafka主机名
    getKafkaHost() {
      if (!this.connectionInfo || !this.connectionInfo.basicInfo) return 'localhost';
      
      // 尝试获取Kafka主节点
      const mainNode = this.connectionInfo.basicInfo['Kafka主节点'];
      if (mainNode) {
        // 提取主机名部分（去掉端口）
        const hostPart = mainNode.split(':')[0];
        return hostPart || 'localhost';
      }
      
      // 尝试从Kafka URL中获取主机名
      const kafkaUrl = this.connectionInfo.basicInfo['kafka_bootstrap_servers'] || 
                       this.connectionInfo.basicInfo['Kafka集群地址'];
      if (kafkaUrl) {
        const brokers = kafkaUrl.split(',');
        if (brokers.length > 0) {
          const firstBroker = brokers[0].trim();
          // 移除协议前缀(如果有)
          const hostPart = firstBroker.replace(/^.*:\/\//, '').split(':')[0];
          return hostPart || 'localhost';
        }
      }
      
      return 'localhost';
    },
    
    // 获取连接信息
    async getConnectionInfo() {
      if (!this.serviceId) return;
      
      this.loading = true;
      this.$emit('loading-change', true);
      
      try {
        const result = await this.$axiosPost(global.API.getConnectionInfo, {
          serviceInstanceId: this.serviceId
        });
        if (result.code === 200) {
          this.connectionInfo = result.data;
          this.$emit('connection-loaded', this.connectionInfo);
        } else {
          this.$message.error(result.msg || '获取连接信息失败');
        }
      } catch (error) {
        console.error('获取连接信息错误:', error);
        this.$message.error('连接信息加载失败，请重试');
      } finally {
        this.loading = false;
        this.$emit('loading-change', false);
      }
    },
    
    // 刷新连接信息
    async refreshConnectionInfo() {
      if (this.refreshing) return;
      
      this.refreshing = true;
      
      try {
        await this.getConnectionInfo();
        this.$message.success('连接信息已更新');
      } catch (error) {
        // 错误已在getConnectionInfo中处理
      } finally {
        this.refreshing = false;
      }
    }
  }
};
</script>

<style lang="less" scoped>
.connection-wrapper {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  padding: 24px;
  position: relative;
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
    margin: 0;
  }
}

.segment-control {
  display: flex;
  background-color: #f5f5f7;
  border-radius: 8px;
  margin-bottom: 24px;
  padding: 4px;
  overflow: hidden;
  
  .segment-item {
    flex: 1;
    text-align: center;
    padding: 10px 16px;
    font-weight: 500;
    color: #86868b;
    cursor: pointer;
    transition: all 0.3s ease;
    border-radius: 6px;
    
    &:hover {
      color: #5E5CE6;
    }
    
    &.active {
      background-color: #ffffff;
      color: #5E5CE6;
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
    }
  }
}

.content-area {
  position: relative;
}

.info-panel {
  min-height: 200px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.loading-container, .empty-container {
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
</style> 