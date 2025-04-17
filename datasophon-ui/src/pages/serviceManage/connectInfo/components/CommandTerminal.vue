<!--
 * @describe: 命令行终端公共组件 - 用于显示命令行示例
-->
<template>
  <div class="command-block-section">
    <div class="code-card">
      <div class="code-header">
        <div class="title-section">
          <span class="code-title">{{ title }}</span>
        </div>
        <div class="header-actions">
          <a-tooltip :title="copyTooltip">
            <a-button 
              type="link" 
              class="action-button copy-button"
              @click="copyAllCommands"
            >
              <a-icon type="copy" />
            </a-button>
          </a-tooltip>
        </div>
      </div>
      <div class="code-content">
        <div class="title-bar">
          <div class="file-name">Terminal</div>
        </div>
        <div class="terminal-container">
          <div class="terminal-content bash-terminal">
            <!-- 命令列表 -->
            <div v-for="(cmd, index) in commandsToShow" :key="index" class="terminal-line-wrapper">
              <!-- 命令注释 -->
              <div class="terminal-line" v-if="cmd.label !== '#'">
                <span class="prompt" v-if="!cmd.commandPrompt">[root@{{ hostName }} {{ serviceHome ? serviceHome.split('/').pop() : '~' }}]#</span>
                <span class="prompt" v-else>{{ cmd.commandPrompt }}</span>
                <span class="command-comment">#{{ cmd.label }}</span>
              </div>
              
              <!-- 命令内容 -->
              <div class="terminal-line" v-if="cmd.value">
                <span class="prompt" v-if="!cmd.commandPrompt">[root@{{ hostName }} {{ serviceHome ? serviceHome.split('/').pop() : '~' }}]#</span>
                <span class="prompt" v-else>{{ cmd.commandPrompt }}</span>
                <span class="command" @click="copySingleCommand(cmd.value)">{{ getCommandDisplay(cmd.value) }}</span>
                <a-tooltip title="复制命令" placement="left">
                  <a-icon type="copy" class="copy-icon" @click.stop="copySingleCommand(cmd.value)" />
                </a-tooltip>
              </div>
              
              <!-- 命令执行结果 -->
              <div v-if="cmd.commandResult" class="terminal-line result-line">
                <span class="command-result">{{ cmd.commandResult }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="status-bar">
          <div class="status-item encoding">UTF-8</div>
          <div class="status-item">LF</div>
          <div class="status-item filetype">bash</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { copyText } from '@/utils/copyUtil';

export default {
  name: "CommandTerminal",
  props: {
    // 命令组标题
    title: {
      type: String,
      default: '常用命令'
    },
    // 命令数组，格式为 [{label: '命令描述', value: '命令内容', commandResult: '命令执行结果'}, ...]
    commands: {
      type: Array,
      default: () => []
    },
    // 终端显示的主机名
    hostName: {
      type: String,
      default: 'localhost'
    },
    // 服务主目录
    serviceHome: {
      type: String,
      default: ''
    },
    // 复制按钮提示文本
    copyTooltip: {
      type: String,
      default: '复制所有命令'
    }
  },
  data() {
    return {
      // 预留一些数据状态
    };
  },
  computed: {
    // 添加一个空的计算属性以保持结构完整
    commandsToShow() {
      return this.commands || [];
    }
  },
  mounted() {
    // 在挂载后调整终端容器高度
    this.adjustTerminalHeight();
    
    // 监听窗口大小改变事件
    window.addEventListener('resize', this.adjustTerminalHeight);
  },
  beforeDestroy() {
    // 移除事件监听器
    window.removeEventListener('resize', this.adjustTerminalHeight);
  },
  methods: {
    // 调整终端高度
    adjustTerminalHeight() {
      const terminalContainer = this.$el.querySelector('.terminal-container');
      if (!terminalContainer) return;
      
      // 获取视窗高度
      const windowHeight = window.innerHeight;
      const containerPosition = terminalContainer.getBoundingClientRect();
      
      // 计算可用空间，留出更少的底部边距
      const availableHeight = windowHeight - containerPosition.top - 50;
      
      // 设置最小高度为600px
      const terminalHeight = Math.max(600, availableHeight);
      
      // 设置高度
      terminalContainer.style.height = `${terminalHeight}px`;
    },
    
    // 获取命令显示（去掉前缀路径，显示更简洁）
    getCommandDisplay(command) {
      if (!command) return '';
      // 如果命令带有绝对路径的bin目录，简化显示
      if (this.serviceHome && command && command.includes(this.serviceHome + '/bin/')) {
        return command.replace(this.serviceHome + '/bin/', 'bin/');
      }
      return command;
    },

    // 复制所有命令到剪贴板
    copyAllCommands() {
      if (!this.commands || this.commands.length === 0) return;
      
      // 构建包含所有命令和注释的文本
      let allCommands = '';
      
      // 添加所有命令
      this.commands.forEach(cmd => {
        if (cmd.label && cmd.value) {
          allCommands += `# ${cmd.label}\n`;
          allCommands += `${cmd.value}\n\n`;
        }
      });
      
      // 使用通用复制工具复制到剪贴板
      copyText(allCommands.trim(), '所有命令', this);
    },
    
    // 复制单条命令
    copySingleCommand(commandText) {
      if (!commandText) return;
      
      // 使用通用复制工具复制到剪贴板
      copyText(commandText, '命令', this);
    }
  }
};
</script>

<style lang="less" scoped>
// 命令终端部分样式
.command-block-section {
  margin-bottom: 24px;
  
  .code-card {
    flex: 1;
    border-radius: 12px;
    overflow: hidden;
    border: 1px solid #e8e8e8;
    background-color: #ffffff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    transition: all 0.3s;
    
    .code-header {
      padding: 12px 16px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: linear-gradient(to right, #f8f9fa, #edf0f5);
      border-bottom: 1px solid rgba(0, 0, 0, 0.05);
      border-top-left-radius: 12px;
      border-top-right-radius: 12px;
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
      
      .title-section {
        display: flex;
        align-items: center;
        flex: 1;
        overflow: hidden;
      }
      
      .code-title {
        font-weight: 600;
        font-size: 15px;
        color: #262626;
        margin-right: 12px;
        letter-spacing: -0.01em;
      }
      
      .header-actions {
        display: flex;
        align-items: center;
        gap: 8px;
        
        .action-button {
          padding: 4px 8px;
          color: #0078d4;
          border-radius: 6px;
          
          &:hover {
            background: rgba(0, 120, 212, 0.1);
          }
          
          &.copy-button {
            color: #0078d4;
          }
        }
      }
    }
    
    .code-content {
      .title-bar {
        padding: 8px 16px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        background: linear-gradient(to right, #eaeef2, #e6e9ee);
        border-bottom: 1px solid rgba(0, 0, 0, 0.06);
        
        .file-name {
          font-family: 'SF Mono', 'Consolas', 'Courier New', monospace;
          font-size: 13px;
          color: #3f3f3f;
          font-weight: 500;
        }
      }
      
      .terminal-container {
        height: 650px;
        overflow-y: auto;
        overflow-x: hidden;
        background-color: #282a36; // 使用Dracula主题背景色，与CodeMirror相同
        position: relative;
        
        /* 自定义滚动条样式 - 透明化处理 */
        &::-webkit-scrollbar {
          width: 6px;
          height: 6px;
        }
        
        &::-webkit-scrollbar-track {
          background: transparent;
        }
        
        &::-webkit-scrollbar-thumb {
          background: rgba(255, 255, 255, 0.1);
          border-radius: 3px;
        }
        
        &::-webkit-scrollbar-thumb:hover {
          background: rgba(255, 255, 255, 0.2);
        }
        
        /* Firefox滚动条样式 */
        scrollbar-width: thin;
        scrollbar-color: rgba(255, 255, 255, 0.1) transparent;
      }
      
      /* 终端内容 */
      .terminal-content {
        padding: 16px 20px;
        font-family: 'SF Mono', 'Consolas', 'Courier New', monospace;
        font-size: 14px;
        white-space: pre-wrap;
        word-break: break-all;
        color: #f8f8f2; // Dracula前景色
        line-height: 1.6;
        tab-size: 4;
        min-height: 100%;
        
        .terminal-line-wrapper {
          margin-bottom: 16px;
          position: relative;
        }
        
        .terminal-line {
          display: flex;
          flex-wrap: nowrap;
          position: relative;
          padding-left: 8px;
          padding-right: 30px; /* 为复制图标留出空间 */
          margin-bottom: 6px;
          
          .prompt {
            color: #50fa7b; // Dracula绿色
            margin-right: 8px;
            user-select: none;
            font-weight: 500;
          }
          
          .command-comment {
            color: #6272a4; // Dracula注释色
            word-break: break-all;
            overflow-wrap: break-word;
            user-select: none;
            font-style: italic;
          }
          
          .command {
            color: #f8f8f2; // Dracula前景色
            word-break: break-all;
            overflow-wrap: break-word;
            cursor: pointer;
            flex: 1;
            
            &:hover {
              text-decoration: underline;
              color: #8be9fd; // Dracula青色
            }
          }
          
          .command-result {
            color: #bd93f9; // Dracula紫色
            word-break: break-all;
            overflow-wrap: break-word;
            padding-left: 16px; /* 缩进结果 */
            font-size: 13px;
          }
          
          .copy-icon {
            position: absolute;
            right: 5px;
            top: 50%;
            transform: translateY(-50%);
            color: rgba(255, 255, 255, 0.3);
            cursor: pointer;
            opacity: 0;
            transition: all 0.2s ease;
            
            &:hover {
              color: #8be9fd; // Dracula青色
              transform: translateY(-50%) scale(1.1);
            }
          }
          
          &:hover .copy-icon {
            opacity: 1;
          }
        }
      }
      
      /* 底部状态栏 */
      .status-bar {
        padding: 4px 16px;
        display: flex;
        align-items: center;
        background-color: #f7f7f7;
        border-top: 1px solid #f0f0f0;
        
        .status-item {
          margin-right: 16px;
          font-size: 12px;
          color: #999;
          
          &.encoding {
            color: #0078d4;
            font-weight: 500;
          }
          
          &:nth-child(2) {
            color: #9061F9;
          }
          
          &.filetype {
            text-transform: uppercase;
            color: #4A9E5C;
            font-weight: 500;
          }
        }
      }
    }
  }
}

/* 媒体查询 */
@media screen and (max-width: 768px) {
  .command-block-section {
    .code-card {
      .code-header {
        padding: 12px 16px;
        
        .code-title {
          font-size: 14px;
        }
      }
      
      .code-content {
        .title-bar {
          height: 30px;
          padding: 0 12px;
        }
        
        .terminal-content {
          font-size: 13px;
          padding: 12px 16px;
        }
        
        .status-bar {
          height: 24px;
          font-size: 11px;
        }
      }
    }
  }
}
</style>