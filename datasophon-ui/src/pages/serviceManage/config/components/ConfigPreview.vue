<template>
  <a-modal
      v-model="visible"
      :title="null"
      width="70%"
      :footer="null"
      :closable="false"
      :maskClosable="true"
      class="preview-modal"
      :destroyOnClose="true"
  >
    <div class="modal-header">
      <div class="modal-file-info">
        <span class="file-type-icon" :class="getFileIconClass(fileName)"></span>
        <span class="modal-file-name">{{ fileName }}</span>
      </div>
      <div class="modal-actions">
        <a-button
            type="link"
            class="modal-action-btn"
            @click="copyContent"
        >
          <a-icon type="copy" />
          复制内容
        </a-button>
        <a-button
            type="link"
            class="modal-action-btn"
            @click="downloadFile"
        >
          <a-icon type="download" />
          下载文件
        </a-button>
        <a-button
            type="link"
            class="modal-close-btn"
            @click="closePreview"
        >
          <a-icon type="close" />
        </a-button>
      </div>
    </div>

    <a-spin :spinning="loading">
      <div class="code-container">
        <div class="code-card">
          <div class="code-header">
            <div class="title-section">
              <span class="code-title">{{ getFileType(fileName) }}</span>
            </div>
            <div class="header-actions">
              <div class="theme-toggle-container">
                <button
                    class="theme-toggle-btn"
                    :class="{ 'active': previewTheme === 'dark' }"
                    @click="previewTheme = 'dark'"
                >
                  <a-icon type="moon" />
                  <span>深色</span>
                </button>
                <button
                    class="theme-toggle-btn"
                    :class="{ 'active': previewTheme === 'light' }"
                    @click="previewTheme = 'light'"
                >
                  <a-icon type="bulb" />
                  <span>浅色</span>
                </button>
              </div>
            </div>
          </div>

          <div class="code-content">
            <div class="title-bar">
              <div class="file-name">{{ fileName }}</div>
            </div>

            <!-- 加载状态 -->
            <div v-if="!editorReady" class="loading-container">
              <a-spin tip="内容加载中...">
                <div class="loading-content"></div>
              </a-spin>
            </div>

            <!-- CodeMirror编辑器 -->
            <div v-show="editorReady" class="editor-container">
              <codemirror
                  v-model="content"
                  :options="cmOptions"
                  class="code-mirror-editor"
                  ref="cmEditor"
              ></codemirror>
            </div>

            <div class="status-bar">
              <div class="status-item encoding">UTF-8</div>
              <div class="status-item">LF</div>
              <div class="status-item filetype">{{ getFileExtension(fileName).toUpperCase() }}</div>
              <div class="status-item position">
                {{ editorInfo.lines }}行 | {{ editorInfo.fileSize }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </a-spin>
  </a-modal>
</template>

<script>
import { codemirror } from 'vue-codemirror';
import CodeMirror from 'codemirror';
import 'codemirror/lib/codemirror.css';
import 'codemirror/theme/dracula.css';

// 导入通用复制工具
import { copyText } from '@/utils/copyUtil';

// 导入各种插件
import 'codemirror/addon/scroll/simplescrollbars.js';
import 'codemirror/addon/scroll/simplescrollbars.css';
import 'codemirror/addon/selection/active-line.js';
import 'codemirror/addon/fold/foldgutter.css';
import 'codemirror/addon/fold/foldcode.js';
import 'codemirror/addon/fold/foldgutter.js';
import 'codemirror/addon/fold/brace-fold.js';
import 'codemirror/addon/fold/indent-fold.js';
import 'codemirror/addon/fold/comment-fold.js';

// 导入语言模式
import 'codemirror/mode/javascript/javascript.js';
import 'codemirror/mode/python/python.js';
import 'codemirror/mode/clike/clike.js';
import 'codemirror/mode/sql/sql.js';
import 'codemirror/mode/shell/shell.js';
import 'codemirror/mode/xml/xml.js';
import 'codemirror/mode/yaml/yaml.js';
import 'codemirror/mode/properties/properties.js';

export default {
  name: 'ConfigPreview',
  components: {
    codemirror
  },
  props: {
    value: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    },
    fileName: {
      type: String,
      default: ''
    },
    fileContent: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      visible: false,
      content: '',
      previewTheme: 'dark',
      editorReady: false,
      cmOptions: {
        mode: 'text/x-yaml',
        theme: 'dracula',
        lineNumbers: true,
        autoCloseBrackets: true,
        matchBrackets: true,
        foldGutter: true,
        gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter'],
        foldOptions: {
          widget: 'triangle',
          marker: 'triangle',
          open: 'triangle',
          closed: 'triangle'
        },
        scrollbarStyle: null,
        viewportMargin: Infinity,
        readOnly: true,
        lineWrapping: true,
        fixedGutter: true,
        styleActiveLine: true,
        tabSize: 4,
        indentWithTabs: false,
        height: 'auto'
      },
      editorInfo: {
        lines: 0,
        fileSize: '0 KB'
      }
    };
  },
  watch: {
    value(val) {
      this.visible = val;
    },
    visible(val) {
      this.$emit('input', val);
      if (val) {
        // 当模态窗口打开时，延时刷新编辑器以确保滚动正常
        this.$nextTick(() => {
          setTimeout(() => {
            if (this.$refs.cmEditor && this.$refs.cmEditor.codemirror) {
              this.refreshEditor();
            }
          }, 300);
        });
      }
    },
    previewTheme(val) {
      this.cmOptions.theme = val === 'dark' ? 'dracula' : 'eclipse';
      this.$nextTick(() => {
        this.refreshEditor();
      });
    },
    fileName(val) {
      if (val) {
        this.updateCodeMirrorMode(val);
      }
    },
    fileContent(val) {
      this.editorReady = false;
      this.content = val;
      this.$nextTick(() => {
        setTimeout(() => {
          this.editorReady = true;
          this.$nextTick(() => {
            this.updateEditorInfo();
            this.refreshEditor();
          });
        }, 200);
      });
    }
  },
  methods: {
    closePreview() {
      this.visible = false;
    },
    copyContent() {
      if (!this.content) {
        this.$message.warning('没有可复制的内容');
        return;
      }

      copyText(this.content, '配置内容', this);
    },
    downloadFile() {
      this.$emit('download');
    },

    refreshEditor() {
      if (!this.$refs.cmEditor || !this.$refs.cmEditor.codemirror) return;

      const editor = this.$refs.cmEditor.codemirror;

      // 强制刷新编辑器
      editor.refresh();

      // 设置编辑器高度
      this.setEditorHeight(editor);
    },

    setEditorHeight(editor) {
      if (!editor) return;

      // 获取行信息
      const totalLines = editor.lineCount();
      const lineHeight = editor.defaultTextHeight();

      // 根据内容设置合适的高度
      let minHeight = 300;
      let maxHeight = Math.min(Math.max(500, totalLines * lineHeight + 30), 600);

      // 设置编辑器高度
      editor.setSize(null, Math.max(minHeight, maxHeight));

      // 设置滚动区域样式
      const scrollElement = editor.getScrollerElement();
      if (scrollElement) {
        scrollElement.style.minHeight = minHeight + 'px';
        scrollElement.style.maxHeight = maxHeight + 'px';
        scrollElement.style.overflow = 'auto';
      }
    },

    // 更新CodeMirror的模式
    updateCodeMirrorMode(fileName) {
      const extension = fileName.split('.').pop().toLowerCase();
      let mode = 'text/plain';

      switch (extension) {
        case 'xml':
          mode = 'application/xml';
          break;
        case 'json':
          mode = 'application/json';
          break;
        case 'yaml':
        case 'yml':
          mode = 'text/x-yaml';
          break;
        case 'properties':
          mode = 'text/x-properties';
          break;
        case 'sh':
          mode = 'text/x-sh';
          break;
        default:
          mode = 'text/plain';
      }

      this.cmOptions.mode = mode;

      // 如果编辑器已存在，直接更新模式
      if (this.$refs.cmEditor && this.$refs.cmEditor.codemirror) {
        this.$refs.cmEditor.codemirror.setOption('mode', mode);
      }
    },

    // 获取文件类型名称
    getFileType(fileName) {
      if (!fileName) return '文本文件';

      const extension = fileName.split('.').pop().toLowerCase();

      const typeMap = {
        xml: 'XML 配置文件',
        json: 'JSON 配置文件',
        properties: 'Properties 配置文件',
        yaml: 'YAML 配置文件',
        yml: 'YAML 配置文件',
        sh: 'Shell 脚本',
        txt: '文本文件',
        conf: '配置文件',
        cfg: '配置文件',
        ini: 'INI 配置文件',
        log: '日志文件'
      };

      return typeMap[extension] || '配置文件';
    },

    // 获取文件图标类名
    getFileIconClass(fileName) {
      if (!fileName) return 'file-icon-default';

      const extension = fileName.split('.').pop().toLowerCase();

      // 配置文件类型
      if (['xml'].includes(extension)) {
        return 'file-icon-xml';
      } else if (['json'].includes(extension)) {
        return 'file-icon-json';
      } else if (['yaml', 'yml'].includes(extension)) {
        return 'file-icon-yaml';
      } else if (['properties', 'prop'].includes(extension)) {
        return 'file-icon-properties';
      } else if (['conf'].includes(extension)) {
        return 'file-icon-conf';
      } else if (['ini'].includes(extension)) {
        return 'file-icon-ini';
      } else if (['acl'].includes(extension)) {
        return 'file-icon-acl';
      }

      // 脚本文件类型
      if (['sh', 'bash', 'zsh'].includes(extension)) {
        return 'file-icon-shell';
      } else if (['bat', 'cmd', 'ps1', 'makefile'].includes(extension)) {
        return 'file-icon-script';
      }

      // 日志文件类型
      if (['log', 'out', 'err', 'trace'].includes(extension)) {
        return 'file-icon-log';
      }

      // 数据文件类型
      if (['csv', 'tsv', 'xlsx', 'xls'].includes(extension)) {
        return 'file-icon-data';
      }

      // 文本文件类型
      if (['txt', 'md', 'markdown', 'rst'].includes(extension)) {
        return 'file-icon-text';
      }

      // 压缩文件类型
      if (['zip', 'tar', 'gz', 'bz2', 'rar', '7z'].includes(extension)) {
        return 'file-icon-archive';
      }

      return 'file-icon-default';
    },

    // 更新编辑器信息
    updateEditorInfo() {
      if (this.content) {
        // 计算行数
        const lines = this.content.split('\n').length;
        this.editorInfo.lines = lines;

        // 计算文件大小
        const bytes = new Blob([this.content]).size;
        this.editorInfo.fileSize = this.formatFileSize(bytes);
      } else {
        this.editorInfo.lines = 0;
        this.editorInfo.fileSize = '0 KB';
      }
    },

    // 格式化文件大小
    formatFileSize(bytes) {
      if (bytes === 0) return '0 KB';

      const k = 1024;
      const sizes = ['B', 'KB', 'MB', 'GB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));

      return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
    },

    // 获取文件扩展名
    getFileExtension(fileName) {
      if (!fileName) return 'txt';
      const parts = fileName.split('.');
      return parts.length > 1 ? parts[parts.length - 1].toLowerCase() : 'txt';
    }
  },
  mounted() {
    // 确保CodeMirror正确初始化
    this.$nextTick(() => {
      // 设置初始内容
      if (this.fileContent) {
        this.content = this.fileContent;
        this.updateEditorInfo();
      }

      // 延迟显示编辑器确保DOM已渲染
      setTimeout(() => {
        this.editorReady = true;
        this.$nextTick(() => {
          this.refreshEditor();
        });
      }, 300);

      // 监听窗口大小变化
      window.addEventListener('resize', () => {
        if (this.visible && this.$refs.cmEditor) {
          this.refreshEditor();
        }
      });
    });
  },
  beforeDestroy() {
    // 移除事件监听
    window.removeEventListener('resize', this.refreshEditor);
  }
};
</script>

<style>
/* 移除所有可能干扰编辑器的全局样式 */
.code-mirror-editor,
.CodeMirror {
  /* 强制启用所有交互 */
  pointer-events: auto !important;
  user-select: auto !important;
  -webkit-user-select: auto !important;
  -moz-user-select: auto !important;
  -ms-user-select: auto !important;

  /* a确保正确的位置和层级 */
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

/* 确保编辑器容器可交互并支持滚动 */
.editor-container {
  overflow: visible !important;
  pointer-events: auto !important;
  user-select: auto !important;
  cursor: text !important;
  position: relative !important;
  z-index: 100 !important;
  min-height: 300px !important;
}

/* 确保CodeMirror编辑器不会溢出其容器，且可以接收鼠标事件 */
.CodeMirror {
  position: relative !important;
  z-index: 1 !important;
  height: auto !important;
  max-height: 600px !important;
  font-family: 'SF Mono', 'Consolas', 'Courier New', monospace !important;
  pointer-events: auto !important; /* 确保可以接收鼠标事件 */
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
</style>

<style scoped>
/* 预览模态窗 */
.preview-modal :deep(.ant-modal-content) {
  border-radius: 16px;
  overflow: hidden;
}

.preview-modal :deep(.ant-modal-body) {
  max-height: 90vh;
  overflow: hidden;
  padding: 24px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.modal-file-info {
  display: flex;
  align-items: center;
}

.file-type-icon {
  width: 32px;
  height: 40px;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  position: relative;
  filter: drop-shadow(0px 2px 3px rgba(0, 0, 0, 0.1));
  transition: all 0.3s ease;
  margin-right: 12px;
}

.modal-file-name {
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.modal-actions {
  display: flex;
  gap: 12px;
}

.modal-action-btn {
  color: #007AFF;
  font-size: 14px;
  border-radius: 6px;
  border: 1px solid rgba(0, 122, 255, 0.1);
  background-color: rgba(0, 122, 255, 0.05);
  padding: 4px 10px;
  transition: all 0.3s ease;
}

.modal-action-btn:hover {
  background-color: rgba(0, 122, 255, 0.1);
  color: #0056b3;
}

.modal-close-btn {
  color: #666;
  font-size: 14px;
  border-radius: 6px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  background-color: rgba(0, 0, 0, 0.02);
  padding: 4px 10px;
  transition: all 0.3s ease;
}

.modal-close-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
  color: #333;
}

/* 主题切换按钮组样式 */
.theme-toggle-container {
  display: flex;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 12px;
  padding: 3px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.theme-toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 14px;
  background: transparent;
  border: none;
  border-radius: 9px;
  font-size: 13px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.645, 0.045, 0.355, 1);
  position: relative;
  outline: none;
}

.theme-toggle-btn > i {
  margin-right: 6px;
  font-size: 14px;
}

.theme-toggle-btn.active {
  background: white;
  color: #222;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.theme-toggle-btn:hover:not(.active) {
  color: #333;
  background: rgba(255, 255, 255, 0.3);
}

/* 代码容器样式 */
.code-container {
  position: relative;
  transition: all 0.3s;
  overflow: visible;
  z-index: 5;
}

.code-card {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e8e8e8;
  background-color: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
}

.code-header {
  padding: 8px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(to right, #f8f9fa, #edf0f5);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 1px 1px rgba(0, 0, 0, 0.02);
}

.title-section {
  display: flex;
  align-items: center;
  flex: 1;
  overflow: hidden;
}

.code-title {
  font-weight: 600;
  font-size: 14px;
  color: #262626;
  letter-spacing: -0.01em;
}

.header-actions {
  display: flex;
  align-items: center;
}

.code-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

.title-bar {
  padding: 4px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(to right, #eaeef2, #e6e9ee);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.file-name {
  font-family: 'SF Mono', 'Consolas', 'Courier New', monospace;
  font-size: 12px;
  color: #3f3f3f;
  font-weight: 500;
}

.editor-container {
  background-color: #282a36;
  min-height: 300px;
  flex: 1;
  overflow: hidden;
  position: relative;
}

.code-mirror-editor {
  height: auto;
  text-align: left;
  min-height: 300px;
}

.status-bar {
  padding: 2px 12px;
  display: flex;
  align-items: center;
  background-color: #f7f7f7;
  border-top: 1px solid #f0f0f0;
  height: 28px;
}

.status-item {
  margin-right: 16px;
  font-size: 12px;
  color: #999;
  font-family: 'SF Mono', SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace;
}

.status-item.encoding {
  color: #0078d4;
  font-weight: 500;
}

.status-item:nth-child(2) {
  color: #9061F9;
}

.status-item.filetype {
  text-transform: uppercase;
  color: #4A9E5C;
  font-weight: 500;
}

.status-item.position {
  margin-left: auto;
  margin-right: 0;
  color: #722ed1;
}

.loading-container {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-content {
  min-height: 80px;
  min-width: 250px;
}

/* 文件图标样式 */
.file-icon-default {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%23A6B0C3" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%23A6B0C3" opacity="0.4" d="M14,2l6,6h-6V2z"/></svg>');
}

.file-icon-conf, .file-icon-properties, .file-icon-ini {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%23FFA726" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%23FFA726" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%23FFA726" d="M10.7,15l-1.5-1.5L10.7,12l-1.4-1.4l-2.9,2.9l2.9,2.9L10.7,15z M17.6,13.5l-2.9-2.9l-1.4,1.4l1.5,1.5l-1.5,1.5l1.4,1.4L17.6,13.5z"/></svg>');
}

.file-icon-xml {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%235C6BC0" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%235C6BC0" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%235C6BC0" d="M10.7,15l-1.5-1.5L10.7,12l-1.4-1.4l-2.9,2.9l2.9,2.9L10.7,15z M17.6,13.5l-2.9-2.9l-1.4,1.4l1.5,1.5l-1.5,1.5l1.4,1.4L17.6,13.5z"/></svg>');
}

.file-icon-json {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%2326C6DA" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%2326C6DA" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%2326C6DA" d="M12,11c-1.1,0-2,0.9-2,2s0.9,2,2,2s2-0.9,2-2S13.1,11,12,11z"/></svg>');
}

.file-icon-yaml {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%238E24AA" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%238E24AA" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%238E24AA" d="M8,13.5l2-2l2,2l2-2l2,2v-2l-2-2l-2,2l-2-2l-2,2V13.5z"/></svg>');
}

.file-icon-shell, .file-icon-script {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%234CAF50" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%234CAF50" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%234CAF50" d="M10.7,15l-1.5-1.5L10.7,12l-1.4-1.4l-2.9,2.9l2.9,2.9L10.7,15z M17,15h-4v-1.5h4V15z"/></svg>');
}

.file-icon-log, .file-icon-text {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%23607D8B" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%23607D8B" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%23607D8B" d="M8,12h8v1.5H8V12z M8,15h8v1.5H8V15z M8,9h4v1.5H8V9z"/></svg>');
}

.file-icon-archive {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%23FF7043" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%23FF7043" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%23FF7043" d="M11,14.5V16h2v-1.5h1V13h-4v1.5H11z M11,9h2v1.5h-2V9z M11,6h2v1.5h-2V6z M11,12h2v-1.5h-2V12z"/></svg>');
}

.file-icon-data {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%232196F3" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%232196F3" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%232196F3" d="M7,13h2v5H7V13z M11,10h2v8h-2V10z M15,15h2v3h-2V15z"/></svg>');
}

.file-icon-acl {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%23F44336" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%23F44336" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%23F44336" d="M16,15h-3v-1.5c0-0.8-0.7-1.5-1.5-1.5S10,12.7,10,13.5V15H8v-1.5c0-1.9,1.6-3.5,3.5-3.5s3.5,1.6,3.5,3.5V15z"/></svg>');
}

/* 自定义滚动条样式 - 苹果风格 */
.code-mirror-editor:deep(::-webkit-scrollbar) {
  width: 12px;
  height: 12px;
}

.code-mirror-editor:deep(::-webkit-scrollbar-track) {
  background: transparent;
  border-radius: 10px;
  margin: 2px;
}

.code-mirror-editor:deep(::-webkit-scrollbar-thumb) {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 10px;
  border: 2px solid transparent;
  background-clip: content-box;
  transition: all 0.2s ease;
}

.code-mirror-editor:deep(::-webkit-scrollbar-thumb:hover) {
  background: rgba(0, 0, 0, 0.5);
  border: 2px solid transparent;
  background-clip: content-box;
}

.code-mirror-editor:deep(::-webkit-scrollbar-corner) {
  background: transparent;
}

/* 深色主题的滚动条 */
.code-mirror-editor:deep(.cm-s-dracula::-webkit-scrollbar-track) {
  background: transparent;
}

.code-mirror-editor:deep(.cm-s-dracula::-webkit-scrollbar-thumb) {
  background: rgba(255, 255, 255, 0.3);
  border: 2px solid transparent;
  background-clip: content-box;
}

.code-mirror-editor:deep(.cm-s-dracula::-webkit-scrollbar-thumb:hover) {
  background: rgba(255, 255, 255, 0.5);
  border: 2px solid transparent;
  background-clip: content-box;
}
</style>