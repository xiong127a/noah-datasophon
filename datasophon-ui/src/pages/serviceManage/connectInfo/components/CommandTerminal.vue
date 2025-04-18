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
              <!-- 命令注释/说明 -->
              <div class="command-label" v-if="cmd.label && cmd.label !== '#'">
                <span class="label-text">{{ cmd.label }}</span>
              </div>
              
              <!-- 命令内容 -->
              <div class="terminal-line command-line" v-if="cmd.value">
                <span class="prompt" v-if="!cmd.commandPrompt">[root@{{ hostName }} {{ serviceHome ? serviceHome.split('/').pop() : '~' }}]#</span>
                <span class="prompt" v-else>{{ cmd.commandPrompt }}</span>
                <span class="command-text-wrapper">
                  <span
                    class="command-text editable"
                    :class="{
                      'active': selectedCommand === cmd.value,
                      'copied': copiedCommand === cmd.value
                    }" 
                    contenteditable="true"
                    @click="setSelectedCommand(cmd.value)"
                    @focus="handleCommandFocus($event, cmd.value)"
                    ref="commandText"
                  >{{ getCommandDisplay(cmd.value) }}</span>
                  <a-tooltip title="复制命令" placement="top">
                    <a-button
                      type="link"
                      size="small"
                      class="copy-btn"
                      :class="{'visible': selectedCommand === cmd.value || isHovering === cmd.value}"
                      @click="copySingleCommand(cmd.value, cmd.label)"
                      @mouseenter="isHovering = cmd.value"
                      @mouseleave="isHovering = null"
                    >
                      <a-icon type="copy" /> <span class="copy-text">点击复制</span>
                    </a-button>
                  </a-tooltip>
                </span>
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
      selection: null,
      // 鼠标悬停的命令
      isHovering: null,
      // 当前活动命令
      activeCommand: null
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

    // 显示复制成功的视觉反馈但不选中文本
    showCopyFeedback(commandText) {
      if (commandText) {
        // 设置被复制的命令，触发动画效果
        this.copiedCommand = commandText;
        
        // 1.5秒后清除复制状态
        setTimeout(() => {
          this.copiedCommand = null;
        }, 1500);
      }
    },
    
    // 设置当前选中的命令
    setSelectedCommand(commandText) {
      this.selectedCommand = commandText;
    },
    
    // 处理命令获得焦点
    handleCommandFocus(event, commandText) {
      this.activeCommand = commandText;
      this.selectedCommand = commandText;
      
      // 创建闪烁的光标效果
      const el = event.target;
      
      // 确保光标在文本末尾
      if (window.getSelection && document.createRange) {
        const range = document.createRange();
        range.selectNodeContents(el);
        range.collapse(false);
        const sel = window.getSelection();
        sel.removeAllRanges();
        sel.addRange(range);
      }
    },
    
    // 选中并直接复制命令文本 - 不再需要此方法，但保留空方法以防其他地方调用
    selectAndCopyCommand(commandText, commandLabel, event) {
      // 函数已被移除，仅保留框架以防有调用
      console.log('selectAndCopyCommand method is deprecated');
    },
    
    // 复制单条命令
    copySingleCommand(commandText, commandLabel) {
      if (!commandText) return;
      
      // 使用微任务确保UI更新后再执行复制
      Promise.resolve().then(() => {
        this.copyTextToClipboard(commandText, '命令', commandLabel, false);
        // 显示复制成功的视觉反馈
        this.showCopyFeedback(commandText);
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
        // 复制全部命令文本，不需要视觉选中
        this.copyTextToClipboard(allCommands.trim(), '所有命令', '', false);
      });
    },
    
    // 文本复制到剪贴板的通用方法
    copyTextToClipboard(text, title, commandLabel = '', showVisualSelection = false) {
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
            
            // 视觉反馈，但不全选文字
            this.showCopyFeedback(text);
            
            // 清除选中状态，延迟以便用户看到视觉反馈
            setTimeout(() => {
              this.clearSelection();
            }, 1000);
          })
          .catch(err => {
            console.warn(`Clipboard API失败(${err.message})，使用DOM方法`);
            // 使用requestAnimationFrame确保在浏览器重绘后再执行DOM操作
            requestAnimationFrame(() => {
              this.legacyCopy(text, title, commandLabel, showVisualSelection); 
            });
          });
        return;
      }
      
      // 回退到传统方法，使用requestAnimationFrame确保在浏览器重绘后再执行
      requestAnimationFrame(() => {
        this.legacyCopy(text, title, commandLabel, showVisualSelection);
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
    legacyCopy(text, title, commandLabel = '', showVisualSelection = false) {
      try {
        // 记录当前滚动位置
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        const scrollLeft = window.pageXOffset || document.documentElement.scrollLeft;
        
        // 创建临时textarea
        const textarea = document.createElement('textarea');
        textarea.value = text;
        
        // 设置样式，使其完全不可见
        textarea.style.cssText = `
          position: fixed !important;
          left: -9999px !important;
          top: -9999px !important;
          width: 1px !important;
          height: 1px !important;
          padding: 0 !important;
          border: none !important;
          outline: none !important;
          box-shadow: none !important;
          background: transparent !important;
          z-index: -9999 !important;
          opacity: 0 !important;
          user-select: text !important;
          overflow: hidden !important;
        `;
        
        // 添加到DOM
        document.body.appendChild(textarea);
        
        // 延迟选择和复制，确保元素已添加到DOM
        setTimeout(() => {
          try {
            // 选择和复制，但不显示选择效果
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
              
              // 视觉反馈但不选中文本
              this.showCopyFeedback(text);
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
        
        .command-label {
          margin-bottom: 4px;
          padding-left: 8px;
          
          .label-text {
            color: #6272a4; // Dracula注释蓝色，更柔和的颜色
            font-weight: normal;
            font-style: italic;
            display: block;
            padding: 2px 8px;
            border-left: 2px solid #6272a4;
            border-radius: 0 2px 2px 0;
            font-size: 13px;
            opacity: 0.9;
          }
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
          
          .command-text-wrapper {
            flex: 1;
            display: flex;
            align-items: center;
            position: relative;
          }
          
          .command-text {
            color: #f8f8f2; // Dracula前景色
            word-break: break-all;
            overflow-wrap: break-word;
            cursor: text;
            position: relative;
            border-radius: 4px;
            padding: 2px 4px;
            transition: all 0.2s ease;
            display: inline;
            
            &.editable {
              min-width: 10px; // 确保有足够空间放置光标
              outline: none;
              
              &:focus {
                background-color: transparent; // 移除背景色
                color: #8be9fd; // 恢复Dracula青色
                caret-color: #50fa7b; // 光标颜色为绿色
              }
            }
            
            &:hover {
              text-decoration: none;
              background-color: transparent; // 保持无背景
              color: #8be9fd; // Dracula青色
            }
            
            &.active {
              background-color: transparent; // 保持无背景
              color: #8be9fd; // 恢复Dracula青色
              text-decoration: none;
            }
            
            &.copied {
              animation: copyPulse 1.5s ease-in-out;
            }
            
            @keyframes copyPulse {
              0% {
                background-color: rgba(24, 144, 255, 0.1);
                color: inherit;
                text-decoration: none;
              }
              20% {
                background-color: rgba(24, 144, 255, 0.25);
                color: #8be9fd;
                text-decoration: none;
              }
              100% {
                background-color: rgba(24, 144, 255, 0);
                color: inherit;
                text-decoration: none;
              }
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
            height: 22px;
            padding: 0 6px;
            margin-left: 6px;
            background-color: rgba(80, 250, 123, 0.15); // 半透明绿色背景
            color: #50fa7b;
            border-radius: 3px;
            font-size: 12px;
            opacity: 0;
            visibility: hidden;
            transition: all 0.2s ease;
            display: flex;
            align-items: center;
            
            .copy-text {
              margin-left: 3px;
              font-size: 12px;
            }
            
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