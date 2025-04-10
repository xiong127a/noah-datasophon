<!--
 * @describe: 命令行终端公共组件 - 用于显示命令行示例
-->
<template>
  <div class="command-card">
    <div class="command-header">
      <span class="command-title">{{ title }}</span>
      <a-tooltip :title="copyTooltip">
        <a-icon
          type="copy"
          class="action-icon"
          @click="copyAllCommands"
        />
      </a-tooltip>
    </div>
    <div class="command-content">
      <div class="title-bar">
        <div class="file-name">Terminal</div>
      </div>
      <div class="terminal-content bash-terminal">
        <!-- 如果提供了服务Home目录，显示进入目录的命令 -->
        <div v-if="serviceHome" class="terminal-line-wrapper">
          <div class="terminal-line">
            <span class="prompt">[root@{{ hostName }} ~]#</span>
            <span class="command-comment">#进入服务目录</span>
          </div>
          <div class="terminal-line">
            <span class="prompt">[root@{{ hostName }} ~]#</span>
            <span class="command" @click="copySingleCommand('cd ' + serviceHome)">cd {{ serviceHome }}</span>
            <a-tooltip title="复制命令" placement="left">
              <a-icon type="copy" class="copy-icon" @click.stop="copySingleCommand('cd ' + serviceHome)" />
            </a-tooltip>
          </div>
        </div>
        
        <!-- 命令列表 -->
        <div v-for="(cmd, index) in commands" :key="index" class="terminal-line-wrapper">
          <!-- 命令注释 -->
          <div class="terminal-line">
            <span class="prompt" v-if="!cmd.commandPrompt">[root@{{ hostName }} {{ serviceHome ? serviceHome.split('/').pop() : '~' }}]#</span>
            <span class="prompt" v-else>{{ cmd.commandPrompt }}</span>
            <span class="command-comment">#{{ cmd.label }}</span>
          </div>
          
          <!-- 命令内容 -->
          <div class="terminal-line">
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
        
        <!-- 最后的命令提示符行 -->
        <div class="terminal-line terminal-prompt-line">
          <span class="prompt" v-if="commands.length > 0 && commands[commands.length-1].commandPrompt && isLastCommandNotQuit">
            {{ commands[commands.length-1].commandPrompt }}
          </span>
          <span class="prompt" v-else>
            [root@{{ hostName }} {{ serviceHome ? serviceHome.split('/').pop() : '~' }}]#
          </span>
          <span class="cursor-wrapper">
            <span class="terminal-cursor"></span>
          </span>
        </div>
      </div>
      <div class="status-bar">
        <div class="status-item encoding">UTF-8</div>
        <div class="status-item">Shell</div>
        <div class="status-item filetype">bash</div>
      </div>
    </div>
  </div>
</template>

<script>
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
  computed: {
    // 判断最后一个命令是否为退出命令
    isLastCommandNotQuit() {
      if (!this.commands || this.commands.length === 0) return true;
      const lastCommand = this.commands[this.commands.length - 1];
      return !(lastCommand.value === '!quit' || lastCommand.label === '退出beeline');
    }
  },
  methods: {
    // 获取命令显示（去掉前缀路径，显示更简洁）
    getCommandDisplay(command) {
      // 如果命令带有绝对路径的bin目录，简化显示
      if (this.serviceHome && command.includes(this.serviceHome + '/bin/')) {
        return command.replace(this.serviceHome + '/bin/', 'bin/');
      }
      return command;
    },

    // 复制所有命令到剪贴板
    copyAllCommands() {
      if (!this.commands || this.commands.length === 0) return;
      
      // 构建包含所有命令和注释的文本
      let allCommands = '';
      
      // 如果有服务目录，先添加cd命令
      if (this.serviceHome) {
        allCommands += `# 进入服务目录\n`;
        allCommands += `cd ${this.serviceHome}\n\n`;
      }
      
      // 添加所有命令
      this.commands.forEach(cmd => {
        allCommands += `# ${cmd.label}\n`;
        allCommands += `${cmd.value}\n\n`;
      });
      
      // 复制到剪贴板
      this.copyText(allCommands.trim());
    },
    
    // 复制单条命令
    copySingleCommand(commandText) {
      if (!commandText) return;
      
      // 复制到剪贴板
      this.copyText(commandText);
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
  
  .terminal-line-wrapper {
    margin-bottom: 12px;
    position: relative;
  }
  
  .terminal-line {
    display: flex;
    flex-wrap: nowrap;
    position: relative;
    padding-left: 8px;
    padding-right: 30px; /* 为复制图标留出空间 */
    margin-bottom: 4px;
    
    .prompt {
      color: #50fa7b;
      margin-right: 8px;
      user-select: none;
      font-weight: bold;
    }
    
    .command-comment {
      color: #6272a4; /* Vim风格的蓝色注释 */
      word-break: break-all;
      overflow-wrap: break-word;
      user-select: none;
      font-style: italic;
    }
    
    .command {
      color: #f8f8f2;
      word-break: break-all;
      overflow-wrap: break-word;
      cursor: pointer;
      flex: 1;
      
      &:hover {
        text-decoration: underline;
        color: #8be9fd;
      }
    }
    
    .command-result {
      color: #bd93f9; /* 紫色显示命令结果 */
      word-break: break-all;
      overflow-wrap: break-word;
      padding-left: 16px; /* 缩进结果 */
      font-family: "SF Mono", SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", Courier, monospace;
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
        color: #8be9fd;
      }
    }
    
    &:hover .copy-icon {
      opacity: 1;
    }
  }
  
  .terminal-prompt-line {
    margin-top: 12px;
    margin-bottom: 0;
    
    .cursor-wrapper {
      position: relative;
      display: inline-block;
      height: 16px;
    }
  }
  
  .terminal-cursor {
    display: inline-block;
    width: 8px;
    height: 16px;
    background-color: rgba(255, 255, 255, 0.7);
    vertical-align: middle;
    animation: blink 1s step-end infinite;
  }
}

@keyframes blink {
  0%, 100% { opacity: 0; }
  50% { opacity: 1; }
}
</style> 