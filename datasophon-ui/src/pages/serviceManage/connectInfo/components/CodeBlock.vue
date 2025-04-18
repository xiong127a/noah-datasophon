<!--
 * @describe: 代码块共享组件 - 用于显示和编辑代码示例，集成CodeMirror
-->
<template>
  <div class="code-block-section">
    <div class="code-container" :class="{'with-dependencies': isDependenciesExpanded}">
      <!-- 代码编辑器区域 -->
    <div class="code-card">
      <div class="code-header">
          <div class="title-section">
        <span class="code-title">{{ title }}</span>
            <!-- 依赖状态标记 -->
            <span v-if="dependencies" class="dependency-badge" @click="toggleDependencies">
              <a-icon type="api" />
              <span class="deps-text">{{ dependenciesSummary || '需要添加依赖' }}</span>
              <a-icon :type="isDependenciesExpanded ? 'caret-up' : 'caret-down'" />
            </span>
          </div>
        <div class="header-actions">
            <!-- 依赖展开按钮 -->
            <a-tooltip v-if="dependencies" :title="isDependenciesExpanded ? '收起依赖' : '展开依赖'">
              <a-button 
                type="link" 
                class="action-button deps-button"
                @click="toggleDependencies"
              >
                <a-icon :type="isDependenciesExpanded ? 'menu-fold' : 'menu-unfold'" />
                {{ isDependenciesExpanded ? '' : '依赖' }}
              </a-button>
            </a-tooltip>
            
          <a-tooltip :title="`点击复制${title}所有代码`">
            <a-button 
              type="link" 
              class="action-button copy-button"
              @click="copyCode"
              :loading="copyingCode"
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
      
      <!-- 依赖信息区域 -->
      <div v-if="dependencies && isDependenciesExpanded" class="dependencies-section">
        <div class="deps-card">
          <div class="deps-header">
            <div class="deps-title">
              {{ getDependencyFileName() }}
              <span class="deps-subtitle">{{ dependenciesSummary || '项目依赖' }}</span>
            </div>
            <a-tooltip title="点击复制依赖">
              <a-button 
                type="link"
                class="deps-copy-btn"
                @click="copyDependencies"
                :loading="copyingDeps"
              >
                <a-icon type="copy" />
              </a-button>
            </a-tooltip>
          </div>
          <div class="deps-content">
            <!-- 使用CodeMirror替换原来的pre标签 -->
            <div v-if="!depEditorReady" class="loading-container">
              <a-spin tip="加载依赖中...">
                <div class="loading-content"></div>
              </a-spin>
            </div>
            <div v-show="depEditorReady" class="editor-container">
              <codemirror
                v-model="dependenciesContent"
                :options="depCmOptions"
                class="code-mirror-editor"
                ref="depCmEditor"
              ></codemirror>
            </div>
            <div class="status-bar">
              <div class="status-item encoding">UTF-8</div>
              <div class="status-item">LF</div>
              <div class="status-item filetype">{{ getDependencyFileType() }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
// 导入vue-codemirror和基础codemirror
import { codemirror } from 'vue-codemirror'
import CodeMirror from 'codemirror'
import 'codemirror/lib/codemirror.css'

// 导入通用复制工具 - 直接静态导入
import { copyText } from '@/utils/copyUtil'

// 导入语言模式
import 'codemirror/mode/javascript/javascript.js'
import 'codemirror/mode/python/python.js' 
import 'codemirror/mode/clike/clike.js'       // Java支持
import 'codemirror/mode/sql/sql.js'           // SQL支持
import 'codemirror/mode/shell/shell.js'       // Shell/Bash支持
import 'codemirror/mode/xml/xml.js'           // XML支持

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

// 导入prismjs用于语法高亮依赖代码
import Prism from 'prismjs'
import 'prismjs/themes/prism.css'
import 'prismjs/components/prism-markup.js'
import 'prismjs/components/prism-xml-doc.js'
import 'prismjs/components/prism-python.js'

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
  if (!code) return code;
  
  try {
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
  } catch (error) {
    console.error('格式化函数错误:', error);
    return code; // 返回原始代码
  }
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
    },
    // 依赖信息
    dependencies: {
      type: String,
      default: ''
    },
    // 依赖摘要信息
    dependenciesSummary: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      // 编辑器内容
      editorContent: '',
      // 依赖内容
      dependenciesContent: '',
      // 原始代码（用于比较）
      originalCode: '',
      // 是否处于编辑模式
      isEditing: false,
      // 是否已编辑
      isEdited: false,
      // 是否显示编辑器（用于控制初始加载和格式化）
      editorReady: false,
      // 依赖编辑器是否准备好
      depEditorReady: false,
      // 是否展开依赖面板
      isDependenciesExpanded: false,
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
        // 允许编辑
        readOnly: false,
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
        // 使用默认滚动条，不使用简约滚动条
        scrollbarStyle: null,
        // 启用自动高度
        viewportMargin: Infinity
      },
      // 依赖编辑器配置选项
      depCmOptions: {
        // 主题
        theme: 'dracula',
        // 启用行号
        lineNumbers: true,
        // 启用代码折叠
        foldGutter: true,
        // 行包装
        lineWrapping: true,
        // 总是只读模式
        readOnly: true,
        // 自动缩进
        smartIndent: true,
        // 当前行高亮
        styleActiveLine: false,
        // 自动高亮匹配的括号
        matchBrackets: true,
        // 显示光标位置信息
        showCursorWhenSelecting: false,
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
      },
      // 复制状态标记
      copyingCode: false,
      copyingDeps: false
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
    },
    // 监听依赖
    dependencies(newDeps) {
      if (newDeps) {
        this.setDependenciesContent(newDeps);
      } else {
        this.dependenciesContent = '';
        this.depEditorReady = true;
      }
    },
    // 监听依赖面板展开状态
    isDependenciesExpanded(newVal) {
      if (newVal && this.dependencies && !this.depEditorReady) {
        this.setDependenciesContent(this.dependencies);
      }
      // 状态改变后重新计算编辑器高度
          this.$nextTick(() => {
        this.setEditorHeight();
        this.setDependencyEditorHeight();
      });
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
    
    // 如果有依赖且已展开，设置依赖内容
    if (this.dependencies && this.isDependenciesExpanded) {
      this.setDependenciesContent(this.dependencies);
    }
  },
  mounted() {
    // 设置语言模式
    this.updateLanguageMode();
    
    // 确保编辑器正确渲染
    this.$nextTick(() => {
      // 强制设置为可编辑模式
      this.forceEnableEditMode();
      
      // 设置高度
      this.setEditorHeight();
      
      // 窗口大小改变时重新计算编辑器高度
      window.addEventListener('resize', this.setEditorHeight, { passive: true });
    });
  },
  beforeDestroy() {
    // 组件销毁前移除事件监听
    window.removeEventListener('resize', this.setEditorHeight);
    
    // 清理所有编辑器实例和DOM
    this.cleanupEditors();
  },
  methods: {
    // 强制启用编辑模式
    forceEnableEditMode() {
      if (this.$refs.cmEditor && this.$refs.cmEditor.codemirror) {
        const editor = this.$refs.cmEditor.codemirror;
        
        // 强制设置为可编辑
        editor.setOption('readOnly', false);
        
        // 设置正确的滚动条样式
        editor.setOption('scrollbarStyle', null);
        
        // 防止编辑器被覆盖层阻挡
        const wrapper = editor.getWrapperElement();
        if (wrapper) {
          wrapper.style.zIndex = '100';
          wrapper.style.position = 'relative';
          wrapper.style.pointerEvents = 'auto';
          wrapper.style.userSelect = 'text';
          
          // 确保滚动区域可滚动
          const scrollArea = wrapper.querySelector('.CodeMirror-scroll');
          if (scrollArea) {
            scrollArea.style.overflow = 'auto';
            scrollArea.style.overflowY = 'auto';
            scrollArea.style.maxHeight = '800px';
          }
        }
        
        // 强制刷新编辑器
        setTimeout(() => {
          editor.refresh();
          console.log('编辑器已强制刷新并设置为可编辑模式');
          
          // 模拟点击事件，确保编辑器获得焦点
          this.isEditing = true;
        }, 200);
      } else {
        console.log('编辑器实例不存在，将在100ms后重试');
        setTimeout(() => {
          this.forceEnableEditMode();
        }, 100);
      }
    },
    // 新增清理方法
    cleanupEditors() {
      // 清理代码编辑器
      if (this.$refs.cmEditor && this.$refs.cmEditor.codemirror) {
        const editor = this.$refs.cmEditor.codemirror;
        editor.toTextArea(); // 将编辑器转换回原始textarea
      }
      
      // 清理依赖编辑器
      if (this.$refs.depCmEditor && this.$refs.depCmEditor.codemirror) {
        const depEditor = this.$refs.depCmEditor.codemirror;
        depEditor.toTextArea(); // 将编辑器转换回原始textarea
      }
      
      // 延迟清理DOM中残留的CodeMirror元素
        setTimeout(() => {
        // 移除所有孤立的CodeMirror相关元素
        const orphanedElements = document.querySelectorAll('body > .CodeMirror, body > pre.CodeMirror-line, body > .CodeMirror-code, body > .CodeMirror-gutter, body > .CodeMirror-linenumber, body > .CodeMirror-cursor, body > textarea.CodeMirror-textarea');
        
        orphanedElements.forEach(element => {
          if (element.parentNode) {
            element.parentNode.removeChild(element);
          }
        });
      }, 100);
    },
    // 格式化语言字符串到CodeMirror语言模式
    formatLanguage(lang) {
      const langMap = {
        'javascript': 'text/javascript',
        'js': 'text/javascript',
        'typescript': 'text/typescript',
        'ts': 'text/typescript',
        'java': 'text/x-java',
        'python': 'text/x-python',
        'py': 'text/x-python',
        'sql': 'text/x-sql',
        'shell': 'text/x-sh',
        'bash': 'text/x-sh'
      };
      
      return langMap[lang.toLowerCase()] || lang;
    },
    
    // 更新编辑器语言模式
    updateLanguageMode() {
      if (this.cmOptions) {
        this.cmOptions.mode = this.formatLanguage(this.language);
        
        // 如果编辑器已经存在，则更新其模式
        if (this.$refs.cmEditor && this.$refs.cmEditor.codemirror) {
          this.$refs.cmEditor.codemirror.setOption('mode', this.formatLanguage(this.language));
        }
      }
    },
    
    // 预处理并格式化代码
    preFormatCode(code) {
      try {
        this.editorReady = false;
        
        // 格式化代码
        // 注意: 在实际环境中，formatCode可能需要使用异步处理，
        // 因为大型代码的格式化可能会阻塞主线程
        let formattedCode = code;
        try {
          formattedCode = formatCode(code, this.formatLanguage(this.language));
        } catch (formatError) {
          console.error('代码格式化函数执行失败:', formatError);
          formattedCode = code; // 使用原始代码
        }
        
        this.editorContent = formattedCode;
        this.originalCode = formattedCode;
        
        this.$nextTick(() => {
          this.editorReady = true;
          // 延迟设置高度确保DOM已更新
          setTimeout(() => {
            this.setEditorHeight();
          }, 100);
        });
      } catch (error) {
        console.error('代码格式化失败:', error);
        // 格式化失败，直接使用原代码
        this.editorContent = code;
        this.originalCode = code;
        this.editorReady = true;
      }
    },
    
    // 设置编辑器高度
    setEditorHeight() {
      if (this.$refs.cmEditor && this.$refs.cmEditor.codemirror) {
        const editor = this.$refs.cmEditor.codemirror;
        
        // 自动调整编辑器高度以适应内容
        const totalLines = editor.lineCount();
        const lineHeight = editor.defaultTextHeight();
        
        // 确保滚动区域可以滚动
        const scrollArea = editor.getScrollerElement();
        if (scrollArea) {
          scrollArea.style.overflow = 'auto';
          scrollArea.style.overflowY = 'auto';
          scrollArea.style.maxHeight = '800px';
        }
        
        // 根据语言类型设置不同的最小高度和最大高度
        let minHeight, maxHeight;
        
        if (this.language === 'shell' || this.language === 'bash') {
          // 命令行使用当前设置，看起来已经不错
          minHeight = 8 * lineHeight;
          maxHeight = Math.min(Math.max((totalLines + 2) * lineHeight, minHeight), 40 * lineHeight);
        } else {
          // Java和Python需要显示更紧凑
          minHeight = 6 * lineHeight; // 减少最小行数
          
          // 计算内容高度，但设置更大的可见行数
          const visibleLines = Math.min(totalLines, 30); // 从15行增加到30行
          const contentHeight = (visibleLines + 1) * lineHeight;
          
          // 设置更大的最大高度限制
          maxHeight = Math.min(Math.max(contentHeight, minHeight), 40 * lineHeight); // 从20行增加到40行
        }
        
        // 应用高度，确保至少达到最小高度
        const calculatedHeight = Math.max(minHeight, maxHeight);
        
        // 设置编辑器容器的最小高度
        const editorContainer = this.$refs.cmEditor.$el;
        if (editorContainer) {
          editorContainer.style.minHeight = minHeight + 'px';
          editorContainer.style.overflow = 'visible';
        }
        
        // 设置编辑器高度
        editor.setSize(null, calculatedHeight);
        
        // 强制刷新编辑器显示
        setTimeout(() => {
          editor.refresh();
        }, 50);
      }
    },
    
    // 获取依赖文件名
    getDependencyFileName() {
      if (this.language === 'java') {
        return 'pom.xml';
      } else if (this.language === 'python') {
        return 'requirements.txt';
      } else {
        return '依赖信息';
      }
    },
    
    // 启用编辑模式
    enableEditMode() {
      if (!this.isEditing) {
        this.isEditing = true;
        
        // 如果编辑器存在，启用编辑模式
        if (this.$refs.cmEditor && this.$refs.cmEditor.codemirror) {
          this.$refs.cmEditor.codemirror.setOption('readOnly', false);
        }
      }
    },
    
    // 代码变更处理
    onCodeChange(newCode) {
      // 检测代码是否有变化
      this.isEdited = newCode !== this.originalCode;
    },
    
    // 复制代码到剪贴板
    copyCode() {
      // 设置加载状态
      this.copyingCode = true;
      
      try {
        // 获取代码文本
        const codeEditor = this.$refs.cmEditor && this.$refs.cmEditor.codemirror;
        if (!codeEditor) {
          console.warn('编辑器实例不存在');
          this.$message.warning('编辑器初始化失败，请手动复制');
          this.copyingCode = false;
          return;
        }
        
        // 获取纯文本内容，统一换行符为\n
        const codeText = codeEditor.getValue().replace(/\r\n/g, '\n');
        console.log('获取到代码内容，长度:', codeText.length);
        
        if (!codeText) {
          this.$message.warning('没有可复制的代码内容');
          this.copyingCode = false;
          return;
        }
        
        // 使用简单直接的复制方法
        this.simpleCopy(codeText, `${this.title}所有代码`);
      } catch (err) {
        console.error('复制初始化异常:', err);
        this.$message.error('复制失败，请手动复制');
        this.copyingCode = false;
      }
    },
    
    // 复制依赖信息到剪贴板
    copyDependencies() {
      if (!this.dependencies) return;
      
      // 设置加载状态
      this.copyingDeps = true;
      
      try {
        // 统一换行符为\n
        const depsText = this.dependencies.replace(/\r\n/g, '\n');
        console.log('获取到依赖内容，长度:', depsText.length);
        
        // 使用简单直接的复制方法
        this.simpleCopy(depsText, '依赖信息');
      } catch (err) {
        console.error('复制依赖初始化异常:', err);
        this.$message.error('复制失败，请手动复制');
        this.copyingDeps = false;
      }
    },
    
    // 简单直接的复制方法
    simpleCopy(text, title) {
      // 1. 首先尝试使用现代API
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text)
          .then(() => {
            console.log(`成功使用Clipboard API复制${title}`);
            this.$message.success(`已复制${title}`);
            this.resetCopyState(title);
          })
          .catch(err => {
            console.log(`Clipboard API失败 (${err.message})，使用备用方法`);
            this.directExecCopy(text, title);
          });
        return;
      }
      
      // 2. 如果不支持现代API，使用直接复制
      this.directExecCopy(text, title);
    },
    
    // 使用execCommand直接复制
    directExecCopy(text, title) {
      try {
        // 记录滚动位置
        const scrollPos = {
          x: window.pageXOffset, 
          y: window.pageYOffset
        };
        
        // 创建一个只存在一瞬间的临时textarea
        const textarea = document.createElement('textarea');
        
        // 设置样式使其不可见但可访问
        textarea.style.position = 'absolute';
        textarea.style.left = '-9999px';
        textarea.style.top = (window.pageYOffset || document.documentElement.scrollTop) + 'px';
        textarea.setAttribute('readonly', 'readonly');
        
        // 设置文本并添加到DOM
        textarea.value = text;
        document.body.appendChild(textarea);
        
        // 选择文本
        textarea.select();
        textarea.setSelectionRange(0, textarea.value.length);
        
        // 执行复制
        const success = document.execCommand('copy');
        
        // 移除textarea
        document.body.removeChild(textarea);
        
        // 恢复滚动位置
        window.scrollTo(scrollPos.x, scrollPos.y);
        
        if (success) {
          console.log(`成功复制${title}`);
          this.$message.success(`已复制${title}`);
        } else {
          console.log(`复制${title}失败，尝试备用方法`);
          this.fallbackCopy(text, title);
        }
        
        this.resetCopyState(title);
      } catch (err) {
        console.error(`复制出错: ${err.message}`);
        this.fallbackCopy(text, title);
      }
    },
    
    // 备用复制方法
    fallbackCopy(text, title) {
      try {
        // 创建input元素备用
        const input = document.createElement('input');
        input.value = text;
        input.style.position = 'fixed';
        input.style.opacity = '0';
        input.style.top = '0';
        input.style.left = '0';
        
        document.body.appendChild(input);
        input.focus();
        input.select();
        
        const success = document.execCommand('copy');
        document.body.removeChild(input);
        
        if (success) {
          this.$message.success(`已复制${title}`);
        } else {
          this.$message.error(`复制${title}失败，请手动复制`);
        }
      } catch (e) {
        console.error(`备用复制方法失败: ${e.message}`);
        this.$message.error(`复制${title}失败，请手动复制`);
      } finally {
        this.resetCopyState(title);
      }
    },
    
    // 重置复制状态
    resetCopyState(title) {
      if (title.includes('依赖')) {
        this.copyingDeps = false;
      } else {
        this.copyingCode = false;
      }
    },
    
    // 切换依赖面板展开/收起状态
    toggleDependencies() {
      this.isDependenciesExpanded = !this.isDependenciesExpanded;
      
      // 如果是展开依赖面板，确保依赖内容已设置
      if (this.isDependenciesExpanded && this.dependencies) {
        this.setDependenciesContent(this.dependencies);
      }
      
      // 依赖面板改变后重新计算编辑器高度
      this.$nextTick(() => {
        this.setEditorHeight();
      });
    },
    
    // 设置依赖内容
    setDependenciesContent(dependencies) {
      if (!dependencies) return;
      
      this.depEditorReady = false;
      this.dependenciesContent = dependencies;
      
      // 设置适当的语言模式
      if (this.depCmOptions) {
        if (this.language === 'java') {
          this.depCmOptions.mode = 'application/xml';
        } else if (this.language === 'python') {
          this.depCmOptions.mode = 'text/x-python';
        } else {
          this.depCmOptions.mode = 'text/plain';
        }
      }
      
      // 确保模板渲染后再初始化编辑器
      this.$nextTick(() => {
        this.depEditorReady = true;
        
        // 再次确保DOM已更新，编辑器实例存在
        this.$nextTick(() => {
          if (this.$refs.depCmEditor && this.$refs.depCmEditor.codemirror) {
            const cm = this.$refs.depCmEditor.codemirror;
            
            // 强制更新编辑器内容和刷新显示
            cm.setValue(dependencies);
            cm.refresh();
            
            // 设置正确的编辑器高度
            this.setDependencyEditorHeight();
          }
        });
      });
    },
    
    // 设置依赖编辑器高度
    setDependencyEditorHeight() {
      if (this.$refs.depCmEditor && this.$refs.depCmEditor.codemirror) {
        const editor = this.$refs.depCmEditor.codemirror;
        
        // 自动调整编辑器高度以适应内容
        const totalLines = editor.lineCount();
        const lineHeight = editor.defaultTextHeight();
        
        // 设置更紧凑的最小高度
        const minHeight = 6 * lineHeight;
        
        // 限制最大显示行数
        const visibleLines = Math.min(totalLines, 12); // 最多显示12行
        const contentHeight = (visibleLines + 1) * lineHeight;
        
        // 限制最大高度
        const maxHeight = Math.min(Math.max(contentHeight, minHeight), 16 * lineHeight);
        
        // 应用高度，确保至少达到最小高度
        const calculatedHeight = Math.max(minHeight, maxHeight);
        
        // 设置编辑器容器的最小高度
        const editorContainer = this.$refs.depCmEditor.$el;
        if (editorContainer) {
          editorContainer.style.minHeight = minHeight + 'px';
        }
        
        // 设置编辑器高度
        editor.setSize(null, calculatedHeight);
        
        // 强制刷新编辑器显示
        setTimeout(() => {
          editor.refresh();
        }, 50);
      }
    },
    
    // 获取依赖文件类型
    getDependencyFileType() {
      if (this.language === 'java') {
        return 'XML';
      } else if (this.language === 'python') {
        return 'TXT';
      } else {
        return 'TXT';
      }
    }
  }
};
</script>

<style lang="less">
/* 移除所有可能干扰编辑器的全局样式 */
.code-mirror-editor,
.CodeMirror {
  /* 强制启用所有交互 */
  pointer-events: auto !important;
  user-select: auto !important;
  -webkit-user-select: auto !important;
  -moz-user-select: auto !important;
  -ms-user-select: auto !important;
  
  /* 确保正确的位置和层级 */
  position: relative !important;
  z-index: 100 !important;
  
  /* 确保可见性 */
  opacity: 1 !important;
  visibility: visible !important;
  
  /* 允许所有内部元素交互 */
  * {
    pointer-events: auto !important;
    user-select: auto !important;
  }
}

/* 修复滚动区域 */
.CodeMirror-scroll {
  overflow: auto !important;
  overflow-x: auto !important;
  overflow-y: auto !important;
  height: auto !important;
  position: relative !important;
  outline: none !important;
  /* 修复触摸设备 */
  -webkit-overflow-scrolling: touch !important;
}

/* 修复编辑器内的所有元素 */
.CodeMirror-scroll,
.CodeMirror-sizer,
.CodeMirror-gutter,
.CodeMirror-gutters,
.CodeMirror-linenumber,
.CodeMirror-lines,
.CodeMirror-line,
.CodeMirror-cursor,
.CodeMirror-selected,
.CodeMirror-code {
  pointer-events: auto !important;
  user-select: auto !important;
  -webkit-user-select: auto !important;
  -moz-user-select: auto !important;
  -ms-user-select: auto !important;
}

/* 确保编辑器容器可交互并支持滚动 */
.editor-container {
  overflow: visible !important;
  pointer-events: auto !important;
  user-select: auto !important;
  cursor: text !important;
  position: relative !important;
  z-index: 100 !important;
  min-height: 150px !important;
}

/* 全局样式：仅隐藏CodeMirror生成的额外textarea，不阻止交互 */
.CodeMirror-code + textarea,
.CodeMirror ~ textarea,
.CodeMirror textarea.CodeMirror-textarea,
body > textarea,
body > textarea.CodeMirror-textarea,
textarea:not([class]):not([id]) {
  position: absolute !important;
  width: 0 !important;
  height: 0 !important;
  opacity: 0 !important;
  overflow: hidden !important;
  z-index: -100 !important;
}

/* 确保CodeMirror编辑器不会溢出其容器，且可以接收鼠标事件 */
.CodeMirror {
  position: relative !important;
  z-index: 1 !important;
  height: auto !important;
  max-height: 1000px !important;
  font-family: 'SF Mono', 'Consolas', 'Courier New', monospace !important;
  pointer-events: auto !important; /* 确保可以接收鼠标事件 */
}

/* 清理直接添加到body的孤立CodeMirror元素 */
body > .CodeMirror,
body > .CodeMirror-scroll,
body > .CodeMirror-sizer,
body > .CodeMirror-gutter,
body > .CodeMirror-gutters,
body > .CodeMirror-linenumber,
body > .CodeMirror-lines,
body > .CodeMirror-cursor,
body > .CodeMirror-code,
body > pre.CodeMirror-line,
body > pre.CodeMirror-line-like,
body > .CodeMirror-measure,
body > .CodeMirror-selected,
body > div[role="presentation"],
body > div[cm-not-content="true"] {
  display: none !important;
  position: absolute !important;
  left: -9999px !important;
  top: -9999px !important;
  height: 0 !important;
  width: 0 !important;
  z-index: -9999 !important;
  opacity: 0 !important;
}

/* 隐藏可能的垃圾文本 */
body > div:not([class]):not([id]),
body > span:not([class]):not([id]),
body > p:not([class]):not([id]),
body > pre:not([class]):not([id]) {
  display: none !important;
}

/* 修复连接信息页面底部可能出现的文本内容 */
.code-block-section + div:not([class]):not([id]),
.code-block-section + pre:not([class]):not([id]),
.code-block-section + textarea:not([class]):not([id]),
.code-block-section ~ div:not([class]):not([id]),
.code-block-section ~ pre:not([class]):not([id]),
.code-block-section ~ textarea:not([class]):not([id]) {
  display: none !important;
}

/* 强制规定代码内容区域，防止内容泄漏 */
.service-connection-info-container {
  position: relative !important;
  overflow: visible !important; /* 修改为visible */
}

/* 限制全局的CodeMirror-line元素，防止它们出现在非编辑器区域 */
body div > pre.CodeMirror-line:not(.CodeMirror .CodeMirror-line),
body .CodeMirror-code > pre.CodeMirror-line:not(.CodeMirror .CodeMirror-code > .CodeMirror-line),
body > pre.CodeMirror-line {
  display: none !important;
}

/* 优化标签栏样式，减少高度占用 */
.connection-info-panel .ant-tabs-nav {
  margin: 0 !important;
  padding: 0 !important;
  min-height: 0 !important;
  height: auto !important;
}

.connection-info-panel .ant-tabs-nav-wrap {
  padding: 0 !important;
}

.connection-info-panel .ant-tabs-tab {
  padding: 4px 14px !important;
  margin: 0 10px 0 0 !important;
  font-size: 14px !important;
  line-height: 1.4 !important;
  min-height: 0 !important;
}

.connection-info-panel .ant-tabs-tabpane {
  padding-top: 0 !important;
}

.connection-info-panel {
  padding: 8px 16px !important;
}

/* 减少标签页内容的顶部边距 */
.tab-content {
  padding: 0 !important;
  margin-top: 0 !important;
}

/* 修复CodeMirror-hscrollbar和CodeMirror-vscrollbar滚动条 */
.CodeMirror-hscrollbar,
.CodeMirror-vscrollbar {
  z-index: 200 !important;
  overflow-x: auto !important;
  overflow-y: auto !important;
  display: block !important;
  opacity: 1 !important;
  visibility: visible !important;
  cursor: default !important;
  position: absolute !important;
  pointer-events: auto !important;
}

.CodeMirror-vscrollbar {
  right: 0 !important;
  top: 0 !important;
  height: 100% !important;
  width: 12px !important;
  overflow-x: hidden !important;
  overflow-y: scroll !important;
}

.CodeMirror-hscrollbar {
  bottom: 0 !important;
  left: 0 !important;
  width: 100% !important;
  height: 12px !important;
  overflow-x: scroll !important;
  overflow-y: hidden !important;
}

/* 确保编辑器容器 */
.editor-container, 
.code-container,
.code-card,
.code-content {
  overflow: visible !important;
  max-height: none !important;
}
</style>

<style lang="less" scoped>
.code-block-section {
  margin-bottom: 8px;
  margin-top: 0;
  position: relative !important;
  overflow: visible !important;
  
  // 代码与依赖容器
  .code-container {
    display: flex;
    gap: 8px;
    position: relative !important;
    transition: all 0.3s;
    overflow: visible !important;
    z-index: 5 !important;
    
    &.with-dependencies {
      margin-bottom: 8px;
    }
  }
  
  // 代码卡片
  .code-card {
    flex: 1;
    border-radius: 4px;
    overflow: hidden !important;
    border: 1px solid #e8e8e8;
    background-color: #ffffff;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
    transition: all 0.3s;
    display: flex !important;
    flex-direction: column !important;
    
    .code-header {
      padding: 4px 10px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: linear-gradient(to right, #f8f9fa, #edf0f5);
      border-bottom: 1px solid rgba(0, 0, 0, 0.05);
      border-top-left-radius: 4px;
      border-top-right-radius: 4px;
      box-shadow: 0 1px 1px rgba(0, 0, 0, 0.02);
      
      .title-section {
        display: flex;
        align-items: center;
        flex: 1;
        overflow: hidden;
      }
      
      .code-title {
        font-weight: 600;
        font-size: 13px;
        color: #262626;
        margin-right: 6px;
        letter-spacing: -0.01em;
      }
      
      .dependency-badge {
        display: inline-flex;
        align-items: center;
        padding: 1px 6px;
        border-radius: 10px;
        background: rgba(0, 120, 212, 0.08);
        color: #0078d4;
        font-size: 11px;
        cursor: pointer;
        transition: all 0.2s;
        max-width: 350px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        
        &:hover {
          background: rgba(0, 120, 212, 0.12);
        }
        
        .deps-text {
          margin: 0 3px;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }
      
      .header-actions {
        display: flex;
        align-items: center;
        gap: 4px;
        
        .action-button {
          padding: 2px 6px;
          color: #0078d4;
          border-radius: 4px;
          
          &:hover {
            background: rgba(0, 120, 212, 0.1);
          }
          
          &.deps-button {
            color: #0078d4;
          }
          
          &.copy-button {
            color: #0078d4;
          }
        }
      }
    }
    
    .code-content {
      display: flex !important;
      flex-direction: column !important;
      flex: 1 !important;
      overflow: hidden !important;
      
      .title-bar {
        padding: 4px 12px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        background: linear-gradient(to right, #eaeef2, #e6e9ee);
        border-bottom: 1px solid rgba(0, 0, 0, 0.06);
        
        .file-name {
          font-family: 'SF Mono', 'Consolas', 'Courier New', monospace;
          font-size: 12px;
          color: #3f3f3f;
          font-weight: 500;
        }
        
        .edit-indicator {
          font-size: 11px;
          color: #ff7875;
          padding: 1px 6px;
          border-radius: 8px;
          background-color: rgba(255, 120, 117, 0.1);
        }
      }
      
      .editor-container {
        background-color: #282a36;
        min-height: 100px; // 减少最小高度
        flex: 1 !important;
        overflow: hidden !important;
        position: relative !important;
        
        .code-mirror-editor {
          height: auto;
          text-align: left;
          min-height: 100px; // 减少最小高度
          
          // 针对Java和Python的特殊样式
          &.java-editor, &.python-editor {
            max-height: 600px; // 从300px增加到600px
          }
          
          // 命令行编辑器保持原样
          &.shell-editor {
            min-height: 120px;
          }
        }
      }
      
      .status-bar {
        padding: 2px 12px;
        display: flex;
        align-items: center;
        background-color: #f7f7f7;
        border-top: 1px solid #f0f0f0;
        
        .status-item {
          margin-right: 12px;
          font-size: 11px;
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
          
          &.modified {
            color: #ff7875;
          }
        }
      }
      
      .loading-container {
        min-height: 150px;
        display: flex;
        align-items: center;
        justify-content: center;
        
        .loading-content {
          min-height: 80px;
          min-width: 250px;
        }
      }
    }
  }
  
  // 依赖信息区域样式
  .dependencies-section {
    width: 35%;
    min-width: 280px;
    position: relative !important;
    z-index: 5 !important;
    
    .deps-card {
      border-radius: 6px;
      overflow: hidden !important;
      border: 1px solid #e8e8e8;
      background-color: #ffffff;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
      height: 100%;
      display: flex !important;
      flex-direction: column !important;
      
      .deps-header {
        padding: 6px 12px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        background: linear-gradient(to right, #f8f9fa, #edf0f5);
        border-bottom: 1px solid rgba(0, 0, 0, 0.05);
        box-shadow: 0 1px 1px rgba(0, 0, 0, 0.02);
        
        .deps-title {
          font-weight: 600;
          font-size: 14px;
          color: #262626;
          display: flex;
          flex-direction: column;
          letter-spacing: -0.01em;
          
          .deps-subtitle {
            font-size: 11px;
            color: #666666;
            margin-top: 1px;
            font-weight: 400;
          }
        }
        
        .deps-copy-btn {
          color: #0078d4;
          padding: 2px 6px;
          border-radius: 4px;
          
          &:hover {
            background: rgba(0, 120, 212, 0.1);
          }
        }
      }
      
      .deps-content {
        padding: 0;
        flex: 1 !important;
        overflow: hidden !important;
        background-color: #282a36;
        display: flex !important;
        flex-direction: column !important;
        position: relative !important;
        
        /* 自定义滚动条样式 - 透明化处理 */
        &::-webkit-scrollbar {
          width: 4px;
          height: 4px;
        }
        
        &::-webkit-scrollbar-track {
          background: transparent;
        }
        
        &::-webkit-scrollbar-thumb {
          background: rgba(255, 255, 255, 0.1);
          border-radius: 2px;
        }
        
        &::-webkit-scrollbar-thumb:hover {
          background: rgba(255, 255, 255, 0.2);
        }
        
        /* Firefox滚动条样式 */
        scrollbar-width: thin;
        scrollbar-color: rgba(255, 255, 255, 0.1) transparent;
        
        .editor-container {
          flex: 1 !important;
          overflow: hidden !important;
          position: relative !important;
          
          .code-mirror-editor {
            height: auto;
            text-align: left;
          }
        }
        
        .loading-container {
          min-height: 120px;
        display: flex;
        align-items: center;
          justify-content: center;
          
          .loading-content {
            min-height: 40px;
            min-width: 180px;
          }
        }
        
        .status-bar {
          padding: 2px 12px;
          display: flex;
          align-items: center;
          background-color: #f7f7f7;
          border-top: 1px solid #f0f0f0;
          
          .status-item {
            margin-right: 12px;
            font-size: 11px;
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
}
</style> 