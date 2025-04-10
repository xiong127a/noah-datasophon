<!--
 * @describe: Hive连接信息组件
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
            title="Java连接示例"
            fileName="Connection.java"
            :code="connectionInfo?.javaCode || ''"
            language="java"
            ref="javaCodeBlock"
          />
        </div>

        <!-- Python 示例代码 -->
        <div v-else-if="activeTab === 2" class="info-panel">
          <CodeBlock
            title="Python连接示例"
            fileName="hive_connection.py"
            :code="connectionInfo?.pythonCode || ''"
            language="python"
            ref="pythonCodeBlock"
          />
        </div>

        <!-- 命令行 -->
        <div v-else-if="activeTab === 3" class="info-panel">
          <div v-for="(cmd, index) in commandLineArray" :key="index" class="command-card">
            <div class="command-header">
              <span class="command-title">{{ cmd.label }}</span>
              <a-tooltip title="复制命令">
                <a-icon
                  type="copy"
                  class="action-icon"
                  @click="copyText(cmd.value)"
                />
              </a-tooltip>
            </div>
            <div class="command-content">
              <div class="title-bar">
                <div class="file-name">Terminal</div>
              </div>
              <div class="terminal-content bash-terminal">
                <div class="terminal-line">
                  <span class="prompt">[root@{{ getHiveServer2Host() }} ~]#</span>
                  <span class="command">{{ cmd.value }}</span>
                </div>
                <div class="terminal-cursor"></div>
              </div>
              <div class="status-bar">
                <div class="status-item encoding">UTF-8</div>
                <div class="status-item">Shell</div>
                <div class="status-item filetype">bash</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import BasicInfo from '../components/BasicInfo';
import CodeBlock from '../components/CodeBlock';

export default {
  name: "HiveConnectInfo",
  components: {
    BasicInfo,
    CodeBlock
  },
  props: {
    serviceId: {
      type: [String, Number],
      required: true
    },
    serviceName: {
      type: String,
      default: ''
    },
    // 接收的连接信息对象
    connectionInfo: {
      type: Object,
      default: () => null
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
    // 检查连接信息是否为空
    isEmpty() {
      if (!this.connectionInfo) return true;
      
      // 检查是否所有关键字段都为空
      const { basicInfo, jdbcUrl, jdbcUrls, javaCode, pythonCode, beelineCommand, cliCommand } = this.connectionInfo;
      return (!basicInfo || Object.keys(basicInfo).length === 0) &&
             !jdbcUrl && (!jdbcUrls || jdbcUrls.length === 0) &&
             !javaCode && !pythonCode && 
             !beelineCommand && !cliCommand;
    },
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
    jdbcUrlArray() {
      if (!this.connectionInfo) return [];
      
      if (this.connectionInfo.jdbcUrls && this.connectionInfo.jdbcUrls.length > 0) {
        return this.connectionInfo.jdbcUrls;
      } else if (this.connectionInfo.jdbcUrl) {
        // 兼容单个JDBC URL的情况
        return [{
          label: 'Hive JDBC URL',
          value: this.connectionInfo.jdbcUrl
        }];
      }
      
      return [];
    },
    commandLineArray() {
      if (!this.connectionInfo) return [];
      
      const commands = [];
      
      if (this.connectionInfo.beelineCommand) {
        commands.push({
          label: 'Beeline 连接命令',
          value: this.connectionInfo.beelineCommand
        });
      }
      
      if (this.connectionInfo.cliCommand) {
        commands.push({
          label: 'Hive CLI 命令',
          value: this.connectionInfo.cliCommand
        });
      }
      
      return commands;
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
    // 获取HiveServer2主机名
    getHiveServer2Host() {
      if (!this.connectionInfo || !this.connectionInfo.basicInfo) return 'localhost';
      
      // 尝试获取HiveServer2主节点
      const mainNode = this.connectionInfo.basicInfo['HiveServer2主节点'];
      if (mainNode) {
        // 提取主机名部分（去掉端口）
        const hostPart = mainNode.split(':')[0];
        return hostPart || 'localhost';
      }
      
      return 'localhost'; // 默认值
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
    },
    
    // 复制文本到剪贴板
    copyText(text) {
      if (!text) return;
      
      // 创建临时textarea元素用于复制
      const textarea = document.createElement('textarea');
      textarea.value = text;
      textarea.setAttribute('readonly', '');
      textarea.style.position = 'absolute';
      textarea.style.left = '-9999px';
      document.body.appendChild(textarea);
      
      // 选择并复制文本
      const selected = document.getSelection().rangeCount > 0 
        ? document.getSelection().getRangeAt(0) 
        : false;
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
      
      // 恢复原始选区
      if (selected) {
        document.getSelection().removeAllRanges();
        document.getSelection().addRange(selected);
      }
      
      // 显示复制成功消息
      this.$message.success('复制成功');
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

// 命令行卡片样式
.command-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid rgba(210, 210, 215, 0.4);
  overflow: hidden;
  margin-bottom: 16px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 0 1px rgba(0, 0, 0, 0.1);
  
  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    transform: translateY(-2px);
  }
  
  .command-header {
    background: linear-gradient(135deg, #5E5CE6 0%, #4E48E0 100%);
    padding: 12px 16px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .command-title {
      font-weight: 500;
      color: white;
      text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
    }
    
    .action-icon {
      color: rgba(255, 255, 255, 0.8);
      cursor: pointer;
      transition: all 0.2s ease;
      
      &:hover {
        color: white;
        transform: scale(1.1);
      }
    }
  }
  
  .command-content {
    padding: 0;
    background: #282a36;
    position: relative;
    
    /* 顶部工具栏 */
    &::before {
      content: "";
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 30px;
      background: #44475a;
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);
      z-index: 1;
    }
    
    /* 标题栏 */
    .title-bar {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 30px;
      z-index: 2;
      display: flex;
      align-items: center;
      justify-content: center;
      
      .file-name {
        font-family: "SF Mono", SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", Courier, monospace;
        font-size: 12px;
        color: rgba(255, 255, 255, 0.7);
        padding: 0 15px;
      }
    }
    
    /* 底部状态栏 */
    .status-bar {
      position: sticky;
      bottom: 0;
      left: 0;
      right: 0;
      height: 22px;
      background: linear-gradient(135deg, #5E5CE6 0%, #4E48E0 100%);
      color: white;
      font-size: 11px;
      display: flex;
      align-items: center;
      font-family: "SF Mono", SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", Courier, monospace;
      z-index: 10;
      
      .status-item {
        padding: 0 10px;
        display: flex;
        align-items: center;
        height: 100%;
        
        &.encoding {
          border-right: 1px solid rgba(255, 255, 255, 0.3);
        }
        
        &.filetype {
          margin-left: auto;
          background-color: rgba(0, 0, 0, 0.15);
        }
      }
    }
  }
}

// 终端样式
.terminal-content {
  margin: 0;
  padding: 45px 16px 22px;  /* 为底部状态栏留出空间 */
  font-family: "SF Mono", SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", Courier, monospace;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
  color: #f8f8f2;
  line-height: 1.5;
  tab-size: 4;
  position: relative;
  min-height: 120px;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  
  .terminal-line {
    display: flex;
    flex-wrap: nowrap;
    margin-bottom: 4px;
    position: relative;
    padding-left: 8px;
    
    .prompt {
      color: #50fa7b;
      margin-right: 8px;
      user-select: none;
      font-weight: bold;
    }
    
    .command {
      color: #f8f8f2;
      word-break: break-all;
      overflow-wrap: break-word;
    }
  }
  
  .terminal-cursor {
    position: absolute;
    bottom: 40px;
    left: 24px;
    width: 8px;
    height: 16px;
    background-color: rgba(255, 255, 255, 0.7);
    animation: cursor-blink 1s step-end infinite;
    opacity: 0.7;
  }
}

@keyframes cursor-blink {
  0%, 100% { opacity: 0; }
  50% { opacity: 1; }
}

// Bash终端特定样式
.bash-terminal {
  background: linear-gradient(160deg, #212121 0%, #424242 100%);
  border-left: 4px solid #616161;
  
  .terminal-line {
    .prompt {
      color: #b9f6ca;
    }
    
    .command {
      color: #e0e0e0;
    }
  }
  
  .terminal-cursor {
    background-color: #b9f6ca;
  }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style> 