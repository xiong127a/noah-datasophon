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
              :loading="copyingAll"
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
              <div class="terminal-line command-line" v-if="cmd.value">
                <span class="prompt" v-if="!cmd.commandPrompt">[root@{{ hostName }} {{ serviceHome ? serviceHome.split('/').pop() : '~' }}]#</span>
                <span class="prompt" v-else>{{ cmd.commandPrompt }}</span>
                <span 
                  class="command-text"
                  :class="{'active': selectedCommand === cmd.value}" 
                  @click="selectAndCopyCommand(cmd.value, cmd.label, $event)" 
                  ref="commandText"
                >{{ getCommandDisplay(cmd.value) }}</span>
                <a-tooltip title="复制命令" placement="left">
                  <a-button
                    type="link"
                    size="small"
                    class="copy-btn"
                    :class="{'visible': selectedCommand === cmd.value}"
                    @click="copySingleCommand(cmd.value, cmd.label)"
                  >
                    <a-icon type="copy" /> 复制
                  </a-button>
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
      // 复制状态
      copyingAll: false,
      copiedCommand: null,
      // 当前选中的命令
      selectedCommand: null,
      // 选择范围
      selection: null
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
    
    // 监听窗口大小改变事件，添加passive标志
    window.addEventListener('resize', this.adjustTerminalHeight, { passive: true });
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

    // 选中并直接复制命令文本
    selectAndCopyCommand(commandText, commandLabel, event) {
      // 设置当前选中的命令
      this.selectedCommand = commandText;
      
      // 阻止事件冒泡但不阻止默认行为
      if (event) {
        event.stopPropagation();
      }
      
      // 使用微任务确保UI更新后再执行复制
      Promise.resolve().then(() => {
        // 直接使用可靠的复制方法
        this.copyTextToClipboard(commandText, '命令', commandLabel);
      });
    },

    // 复制单条命令
    copySingleCommand(commandText, commandLabel) {
      if (!commandText) return;
      
      // 使用微任务确保UI更新后再执行复制
      Promise.resolve().then(() => {
        this.copyTextToClipboard(commandText, '命令', commandLabel);
      });
    },
    
    // 复制所有命令到剪贴板
    copyAllCommands() {
      if (!this.commands || this.commands.length === 0) return;
      
      this.copyingAll = true;
      
      // 构建包含所有命令和注释的文本
      let allCommands = '';
      
      // 添加所有命令
      this.commands.forEach(cmd => {
        if (cmd.label && cmd.value) {
          allCommands += `# ${cmd.label}\n`;
          allCommands += `${cmd.value}\n\n`;
        }
      });

      // 使用微任务确保UI更新后再执行复制
      Promise.resolve().then(() => {
        // 复制全部命令文本
        this.copyTextToClipboard(allCommands.trim(), '所有命令');
      });
    },
    
    // 文本复制到剪贴板的通用方法
    copyTextToClipboard(text, title, commandLabel = '') {
      if (!text) return;
      
      // 设置加载状态
      if (title === '所有命令') {
        this.copyingAll = true;
      }
      
      console.log(`开始复制${title}，长度:${text.length}`);
      
      // 准备成功消息
      const successMessage = this.getSuccessMessage(title, commandLabel);
      
      // 尝试使用Clipboard API (最现代的方法)
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text)
          .then(() => {
            console.log(`使用Clipboard API成功复制${title}`);
            this.$message.success(successMessage);
            
            if (title === '所有命令') {
              this.copyingAll = false;
            }
            
            // 清除选中状态，延迟以便用户看到视觉反馈
            setTimeout(() => {
              this.clearSelection();
            }, 1000);
          })
          .catch(err => {
            console.warn(`Clipboard API失败(${err.message})，使用DOM方法`);
            // 使用requestAnimationFrame确保在浏览器重绘后再执行DOM操作
            requestAnimationFrame(() => {
              this.legacyCopy(text, title, commandLabel); 
            });
          });
        return;
      }
      
      // 回退到传统方法，使用requestAnimationFrame确保在浏览器重绘后再执行
      requestAnimationFrame(() => {
        this.legacyCopy(text, title, commandLabel);
      });
    },
    
    // 获取复制成功消息
    getSuccessMessage(title, commandLabel) {
      if (title === '所有命令') {
        return `已复制${title}`;
      } else if (commandLabel) {
        return `已复制"${commandLabel}"的命令`;
      } else {
        return `已复制${title}`;
      }
    },
    
    // 传统复制方法 (DOM方法)
    legacyCopy(text, title, commandLabel = '') {
      try {
        // 记录当前滚动位置
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        const scrollLeft = window.pageXOffset || document.documentElement.scrollLeft;
        
        // 创建临时textarea
        const textarea = document.createElement('textarea');
        textarea.value = text;
        
        // 设置样式，使用absolute而非fixed，相对于当前滚动位置
        textarea.style.cssText = `
          position: absolute !important;
          left: ${scrollLeft}px !important;
          top: ${scrollTop}px !important;
          width: 2em !important;
          height: 2em !important;
          padding: 0 !important;
          border: none !important;
          outline: none !important;
          box-shadow: none !important;
          background: transparent !important;
          z-index: 999999 !important;
          opacity: 0.01 !important;
          user-select: text !important;
        `;
        
        // 添加到DOM
        document.body.appendChild(textarea);
        
        // 延迟选择和复制，确保元素已添加到DOM
        setTimeout(() => {
          try {
            // 选择和复制
            textarea.focus();
            textarea.select();
            
            // 执行复制
            const successful = document.execCommand('copy');
            
            // 获取成功消息
            const successMessage = this.getSuccessMessage(title, commandLabel);
            
            // 反馈
            if (successful) {
              console.log(`成功使用DOM方法复制${title}`);
              this.$message.success(successMessage);
            } else {
              console.error(`DOM复制${title}失败`);
              this.$message.error(`复制${title}失败，请手动复制`);
            }
          } catch (innerErr) {
            console.error(`复制操作过程中出错:`, innerErr);
            this.$message.error(`复制${title}失败，请手动复制`);
          } finally {
            // 清理DOM
            if (document.body.contains(textarea)) {
              document.body.removeChild(textarea);
            }
            
            // 恢复滚动位置
            window.scrollTo(scrollLeft, scrollTop);
            
            // 重置状态
            if (title === '所有命令') {
              this.copyingAll = false;
            }
            
            // 清除选中状态，延迟以便用户看到视觉反馈
            setTimeout(() => {
              this.clearSelection();
            }, 1000);
          }
        }, 10);
      } catch (err) {
        console.error(`复制${title}出错:`, err);
        this.$message.error(`复制${title}失败，请手动复制`);
        
        if (title === '所有命令') {
          this.copyingAll = false;
        }
        
        this.clearSelection();
      }
    },
    
    // 清除选中状态
    clearSelection() {
      this.selectedCommand = null;
      
      if (window.getSelection) {
        window.getSelection().removeAllRanges();
      }
      
      this.selection = null;
    },
    
    // 兼容旧方法调用
    directCopy(text, title) {
      this.copyTextToClipboard(text, title);
    },
    
    githubStyleCopy(text, title) {
      this.copyTextToClipboard(text, title);
    },
    
    execCommandCopy(text, title) {
      this.copyTextToClipboard(text, title);
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
          padding-right: 100px; /* 为复制按钮留出更多空间 */
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
          
          .command-text {
            color: #f8f8f2; // Dracula前景色
            word-break: break-all;
            overflow-wrap: break-word;
            cursor: pointer;
            flex: 1;
            position: relative;
            padding: 2px 4px;
            border-radius: 3px;
            transition: all 0.2s ease;
            
            &:hover {
              text-decoration: underline;
              background-color: rgba(139, 233, 253, 0.1); // 浅色背景
              color: #8be9fd; // Dracula青色
            }
            
            &.active {
              background-color: rgba(139, 233, 253, 0.2); // 选中状态背景色
              text-decoration: underline;
              outline: 1px dashed rgba(255, 255, 255, 0.3);
            }
          }
          
          .command-result {
            color: #bd93f9; // Dracula紫色
            word-break: break-all;
            overflow-wrap: break-word;
            padding-left: 16px; /* 缩进结果 */
            font-size: 13px;
          }
          
          // 复制按钮
          .copy-btn {
            position: absolute;
            right: 5px;
            top: 50%;
            transform: translateY(-50%);
            background-color: rgba(80, 250, 123, 0.15); // 半透明绿色背景
            color: #50fa7b;
            border-radius: 3px;
            padding: 0 5px;
            font-size: 12px;
            height: 20px;
            line-height: 20px;
            opacity: 0;
            visibility: hidden;
            transition: all 0.2s ease;
            
            &:hover {
              background-color: rgba(80, 250, 123, 0.3);
              color: #f8f8f2;
            }
            
            &.visible {
              opacity: 1;
              visibility: visible;
            }
          }
          
          // 显示复制按钮
          &:hover .copy-btn {
            opacity: 0.8;
            visibility: visible;
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