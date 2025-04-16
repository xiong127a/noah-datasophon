<!--
 * @describe: 代码块共享组件 - 用于显示和编辑代码示例，集成CodeMirror
-->
<template>
  <div class="code-block-section">
    <div class="code-card">
      <div class="code-header">
        <span class="code-title">{{ title }}</span>
        <div class="header-actions">
          <a-tooltip :title="`复制${title}`">
            <a-button 
              type="link" 
              class="action-button copy-button"
              @click="copyCode"
            >
              <a-icon type="copy" />
            </a-button>
          </a-tooltip>
        </div>
      </div>
      <div class="code-content">
        <div class="title-bar">
          <div class="file-name">{{ fileName }}</div>
          <div v-if="isEditing" class="edit-indicator">编辑模式</div>
        </div>
        
        <!-- 加载状态 -->
        <div v-if="!editorReady" class="loading-container">
          <a-spin tip="代码格式化中...">
            <div class="loading-content"></div>
          </a-spin>
        </div>
        
        <!-- CodeMirror编辑器 -->
        <div v-show="editorReady" class="editor-container" @click="enableEditMode">
          <codemirror
            v-model="editorContent"
            :options="cmOptions"
            class="code-mirror-editor"
            @input="onCodeChange"
            ref="cmEditor"
          ></codemirror>
        </div>
        
        <div class="status-bar">
          <div class="status-item encoding">UTF-8</div>
          <div class="status-item">LF</div>
          <div class="status-item filetype">{{ language }}</div>
          <div v-if="isEdited" class="status-item modified">已修改</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
// 导入vue-codemirror和基础codemirror
import { codemirror } from 'vue-codemirror'
import 'codemirror/lib/codemirror.css'

// 导入语言模式
import 'codemirror/mode/javascript/javascript.js'
import 'codemirror/mode/python/python.js' 
import 'codemirror/mode/clike/clike.js'       // Java支持
import 'codemirror/mode/sql/sql.js'           // SQL支持
import 'codemirror/mode/shell/shell.js'       // Shell/Bash支持

// 导入主题
import 'codemirror/theme/dracula.css'
import 'codemirror/theme/material.css'
import 'codemirror/theme/monokai.css'

// 导入附加功能
import 'codemirror/addon/fold/foldgutter.css'
import 'codemirror/addon/fold/foldcode.js'
import 'codemirror/addon/fold/foldgutter.js'
import 'codemirror/addon/fold/brace-fold.js'
import 'codemirror/addon/fold/indent-fold.js'  // 基于缩进的折叠
import 'codemirror/addon/fold/comment-fold.js' // 注释折叠

// 自动闭合括号和标签
import 'codemirror/addon/edit/closebrackets.js'
import 'codemirror/addon/edit/matchbrackets.js'

// 行号和当前行高亮
import 'codemirror/addon/selection/active-line.js'

// 导入格式化相关插件
import 'codemirror/addon/comment/comment.js'  // 注释处理
import 'codemirror/addon/edit/trailingspace.js'  // 尾随空格显示
import 'codemirror/addon/edit/continuelist.js'  // 多行编辑支持
import 'codemirror/addon/display/placeholder.js'  // 占位符

// 导入格式化工具函数
function formatCode(code, mode) {
  // 创建临时DOM元素用于格式化
  const tempTextArea = document.createElement('textarea');
  document.body.appendChild(tempTextArea);
  
  // 创建临时CodeMirror实例
  const tempEditor = CodeMirror.fromTextArea(tempTextArea, {
    mode: mode,
    indentUnit: 4,
    smartIndent: true,
    tabSize: 4,
    indentWithTabs: false,
    lineNumbers: false
  });
  
  // 设置代码并格式化
  tempEditor.setValue(code);
  
  // 执行自动格式化
  const totalLines = tempEditor.lineCount();
  for (let i = 0; i < totalLines; i++) {
    tempEditor.indentLine(i);
  }
  
  // 获取格式化后的代码
  const formattedCode = tempEditor.getValue();
  
  // 清理临时DOM元素
  document.body.removeChild(tempTextArea);
  
  return formattedCode;
}

export default {
  name: "CodeBlock",
  components: {
    codemirror
  },
  props: {
    // 代码标题
    title: {
      type: String,
      default: '代码示例'
    },
    // 文件名显示
    fileName: {
      type: String,
      default: 'example.txt'
    },
    // 代码内容
    code: {
      type: String,
      default: ''
    },
    // 语言类型，用于语法高亮
    language: {
      type: String,
      default: 'java'
    },
    // 是否自动格式化代码
    autoFormat: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      // 编辑器内容
      editorContent: '',
      // 原始代码（用于比较）
      originalCode: '',
      // 是否处于编辑模式
      isEditing: false,
      // 是否已编辑
      isEdited: false,
      // 是否显示编辑器（用于控制初始加载和格式化）
      editorReady: false,
      // CodeMirror配置选项
      cmOptions: {
        // 主题
        theme: 'dracula',
        // 启用行号
        lineNumbers: true,
        // 启用代码折叠
        foldGutter: true,
        // 行包装
        lineWrapping: true,
        // 默认为只读模式
        readOnly: true,
        // 根据语言类型设置语言模式，默认为java
        mode: 'text/x-java',
        // 自动高亮匹配的括号
        matchBrackets: true,
        // 自动缩进
        smartIndent: true,
        // 当前行高亮
        styleActiveLine: true,
        // 显示光标位置信息
        showCursorWhenSelecting: true,
        // 自动括号匹配
        autoCloseBrackets: true,
        // 缩进单位
        tabSize: 4,
        // 启用代码折叠指示器
        gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter'],
        // 缩进使用空格
        indentWithTabs: false,
        // 简单滚动条配置
        scrollbarStyle: 'null',
        // 启用自动高度
        viewportMargin: Infinity
      }
    };
  },
  watch: {
    // 监听输入代码变化
    code(newCode) {
      // 预处理代码 - 在设置到编辑器之前进行格式化
      if (this.autoFormat && newCode) {
        // 使用预处理函数格式化代码
        this.preFormatCode(newCode);
      } else {
        // 直接设置内容，不格式化
        this.editorContent = newCode || '';
        this.originalCode = newCode || '';
      }
    },
    // 监听语言变化
    language() {
      this.updateLanguageMode();
    }
  },
  created() {
    // 在实例创建时导入CodeMirror滚动条样式扩展，使用简约滚动条
    require('codemirror/addon/scroll/simplescrollbars.js');
    require('codemirror/addon/scroll/simplescrollbars.css');
    
    // 预处理初始代码
    if (this.autoFormat && this.code) {
      this.preFormatCode(this.code);
    } else {
      this.editorContent = this.code || '';
      this.originalCode = this.code || '';
      this.editorReady = true;
    }
  },
  mounted() {
    // 设置语言模式
    this.updateLanguageMode();
    
    // 确保编辑器正确渲染
    this.$nextTick(() => {
      if (this.$refs.cmEditor && this.$refs.cmEditor.codemirror) {
        const cm = this.$refs.cmEditor.codemirror;
        
        // 设置固定高度而不是最小/最大高度
        const windowHeight = window.innerHeight;
        // 计算适合屏幕的高度，保证足够大的垂直空间
        const editorPosition = cm.getWrapperElement().getBoundingClientRect();
        const availableHeight = windowHeight - editorPosition.top - 80;
        
        // 确保高度不小于500px
        const editorHeight = Math.max(500, availableHeight);
        
        // 直接设置固定高度
        cm.getWrapperElement().style.height = `${editorHeight}px`;
        
        // 强制刷新确保渲染正确
        setTimeout(() => {
          cm.refresh();
        }, 100);
      }
    });
  },
  methods: {
    // 预处理代码格式化 - 在代码显示之前进行格式化
    preFormatCode(code) {
      if (!code) return;
      
      try {
        // 获取当前模式
        let mode = this.getLanguageMode();
        
        // 创建临时DOM元素用于格式化
        const textArea = document.createElement('textarea');
        document.body.appendChild(textArea);
        
        // 创建一个隐藏的CodeMirror实例来格式化代码
        const CodeMirror = window.CodeMirror || require('codemirror');
        const tempEditor = CodeMirror.fromTextArea(textArea, {
          mode: mode,
          indentUnit: 4,
          smartIndent: true,
          tabSize: 4,
          indentWithTabs: false
        });
        
        // 设置代码
        tempEditor.setValue(code);
        
        // 格式化所有行
        const totalLines = tempEditor.lineCount();
        for (let i = 0; i < totalLines; i++) {
          tempEditor.indentLine(i);
        }
        
        // 获取格式化后的代码
        const formattedCode = tempEditor.getValue();
        
        // 清理临时元素
        tempEditor.toTextArea();
        document.body.removeChild(textArea);
        
        // 设置到编辑器
        this.editorContent = formattedCode;
        this.originalCode = formattedCode;
      } catch (error) {
        console.error('代码格式化失败:', error);
        // 出错时直接使用原始代码
        this.editorContent = code;
        this.originalCode = code;
      }
      
      // 标记编辑器已准备好
      this.editorReady = true;
    },
    
    // 复制代码到剪贴板
    copyCode() {
      this.$copyText(this.editorContent).then(
        () => {
          this.$message.success(`${this.title}已复制到剪贴板`);
        },
        () => {
          this.$message.error('复制失败，请手动选择并复制');
        }
      );
    },
    
    // 切换到编辑模式
    enableEditMode() {
      if (!this.isEditing) {
        this.isEditing = true;
        // 启用编辑模式
        if (this.$refs.cmEditor && this.$refs.cmEditor.codemirror) {
          this.$refs.cmEditor.codemirror.setOption('readOnly', false);
          // 设置焦点
          this.$refs.cmEditor.codemirror.focus();
        }
      }
    },
    
    // 监听代码变化
    onCodeChange(newCode) {
      this.isEdited = newCode !== this.originalCode;
    },
    
    // 获取当前语言的模式
    getLanguageMode() {
      // 根据语言类型设置相应的模式
      switch (this.language.toLowerCase()) {
        case 'java':
          return 'text/x-java';
        case 'javascript':
        case 'js':
          return 'text/javascript';
        case 'python':
        case 'py':
          return 'text/x-python';
        case 'sql':
          return 'text/x-sql';
        case 'bash':
        case 'shell':
          return 'text/x-sh';
        default:
          return 'text/plain';
      }
    },
    
    // 更新语言模式
    updateLanguageMode() {
      const mode = this.getLanguageMode();
      
      // 更新选项
      this.cmOptions.mode = mode;
      
      // 如果编辑器已实例化，直接设置模式
      if (this.$refs.cmEditor && this.$refs.cmEditor.codemirror) {
        this.$refs.cmEditor.codemirror.setOption('mode', mode);
        this.$refs.cmEditor.codemirror.refresh();
      }
    }
  }
};
</script>

<style lang="less">
/* 全局CSS变量 */
:root {
  --editor-bg: #282c34;
  --editor-text: #abb2bf;
  --editor-line-number: #636d83;
  --editor-gutter-bg: #21252b;
  --editor-cursor: #528bff;
  --editor-selection: rgba(100, 100, 100, 0.33);
  --editor-active-line: rgba(0, 0, 0, 0.2);
  --editor-matched-bracket: rgba(255, 255, 255, 0.25);
  --editor-scrollbar-thumb: #4e566a;
  --editor-scrollbar-track: #282c34;
  --editor-hint-bg: #282c34;
  --editor-hint-border: #181a1f;
  --editor-overlay-bg: rgba(0, 0, 0, 0.5);
  --editor-tooltip-bg: #282c34;
  --editor-tooltip-border: #181a1f;
  --editor-header-bg: #5E5CE6;
  --editor-border-radius: 12px;
  --editor-button-hover: rgba(255, 255, 255, 0.1);
}

/* 代码编辑器卡片样式 */
.code-block-section {
  margin-bottom: 20px;
  
  .code-card {
    background: #ffffff;
    border-radius: var(--editor-border-radius);
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 0 1px rgba(0, 0, 0, 0.1);
    transition: all 0.3s ease;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    }
    
    .code-header {
      background: linear-gradient(135deg, #5E5CE6 0%, #4E48E0 100%);
      padding: 12px 16px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .code-title {
        font-weight: 500;
        color: white;
        text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
      }
      
      .header-actions {
        display: flex;
        gap: 8px;
        
        .action-button {
          color: white;
          padding: 4px 8px;
          border-radius: 4px;
          background: rgba(255, 255, 255, 0.1);
          border: none;
          transition: all 0.2s ease;
          
          &:hover {
            background: rgba(255, 255, 255, 0.2);
            transform: scale(1.05);
          }
          
          &:active {
            transform: scale(0.95);
          }
        }
      }
    }
    
    .code-content {
      position: relative;
      padding: 0;
      background: var(--editor-bg);
      
      /* 加载状态容器 */
      .loading-container {
        display: flex;
        justify-content: center;
        align-items: center;
        min-height: 300px;
        background: var(--editor-bg);
        
        .loading-content {
          min-height: 200px;
          min-width: 200px;
        }

        /* 自定义加载图标颜色为紫色以匹配主题 */
        .ant-spin-dot i {
          background-color: #5E5CE6;
        }
        
        /* 自定义加载文字颜色为浅色以匹配暗背景 */
        .ant-spin-text {
          color: #abb2bf;
          margin-top: 10px;
          font-size: 14px;
        }
      }
      
      /* 顶部工具栏 */
      .title-bar {
        position: sticky;
        top: 0;
        left: 0;
        right: 0;
        height: 30px;
        background: var(--editor-gutter-bg);
        border-bottom: 1px solid rgba(255, 255, 255, 0.08);
        z-index: 10;
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0 15px;
        
        .file-name {
          font-family: "SF Mono", SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", Courier, monospace;
          font-size: 12px;
          color: rgba(255, 255, 255, 0.7);
        }
        
        .edit-indicator {
          font-size: 12px;
          color: #ff9500;
          background: rgba(255, 149, 0, 0.15);
          padding: 2px 8px;
          border-radius: 4px;
          animation: pulse 2s infinite;
        }
      }
      
      /* 编辑器容器 */
      .editor-container {
        cursor: text;
        position: relative;
        
        &::after {
          content: "点击编辑";
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%);
          background: rgba(0, 0, 0, 0.6);
          color: white;
          padding: 8px 16px;
          border-radius: 4px;
          font-size: 14px;
          opacity: 0;
          transition: opacity 0.3s ease;
          pointer-events: none;
        }
        
        &:hover::after {
          opacity: 1;
        }
      }
      
      /* CodeMirror编辑器样式覆盖 */
      .code-mirror-editor {
        height: auto;
        
        /* 编辑器根容器 */
        .CodeMirror {
          /* 使用固定高度替代min/max-height */
          height: 600px !important;
          font-family: "SF Mono", SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", Courier, monospace;
          line-height: 1.5;
          font-size: 13px;
          color: var(--editor-text);
          background: var(--editor-bg);
          border: none;
          border-radius: 0;
          
          /* 编辑器滚动容器 */
          .CodeMirror-scroll {
            /* 只保留垂直滚动 */
            overflow-y: auto !important;
            overflow-x: hidden !important;
          }
          
          /* 编辑器内容区域 */
          .CodeMirror-lines {
            padding: 16px 0;
          }
          
          /* 行号区域 */
          .CodeMirror-gutters {
            background: var(--editor-gutter-bg);
            border-right: 1px solid rgba(255, 255, 255, 0.05);
          }
          
          /* 行号 */
          .CodeMirror-linenumber {
            color: var(--editor-line-number);
          }
          
          /* 光标 */
          .CodeMirror-cursor {
            border-left: 2px solid var(--editor-cursor);
          }
          
          /* 当前行高亮 */
          .CodeMirror-activeline-background {
            background: var(--editor-active-line);
          }
          
          /* 选中文本背景 */
          .CodeMirror-selected {
            background: var(--editor-selection);
          }
          
          /* 匹配的括号 */
          .CodeMirror-matchingbracket {
            background: var(--editor-matched-bracket);
            color: #fff !important;
            border-bottom: 1px solid #528bff;
          }
          
          /* 代码折叠 */
          .CodeMirror-foldgutter {
            width: 15px;
          }
          
          .CodeMirror-foldgutter-open:after {
            content: "▾";
          }
          
          .CodeMirror-foldgutter-folded:after {
            content: "▸";
          }
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
            text-transform: uppercase;
          }
          
          &.modified {
            margin-left: auto;
            background-color: rgba(255, 149, 0, 0.7);
            color: white;
            font-weight: bold;
            animation: pulse 2s infinite;
          }
        }
      }
    }
  }
}

/* 动画效果 */
@keyframes pulse {
  0% { opacity: 0.7; }
  50% { opacity: 1; }
  100% { opacity: 0.7; }
}
</style> 