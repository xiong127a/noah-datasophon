<template>
  <span class="file-icon" :class="iconClass" :style="iconStyle">
    <span v-if="showExtension && fileExtension" class="file-extension">{{ fileExtension }}</span>
  </span>
</template>

<script>
export default {
  name: 'FileIcon',
  props: {
    fileName: {
      type: String,
      required: true
    },
    size: {
      type: [Number, String],
      default: 24
    },
    showExtension: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    fileExtension() {
      if (!this.fileName) return '';
      const parts = this.fileName.split('.');
      return parts.length > 1 ? parts.pop().toLowerCase() : '';
    },
    
    fileType() {
      if (!this.fileName) return 'default';
      
      const extension = this.fileExtension;
      
      // 配置文件类型
      const configTypes = ['xml', 'json', 'yaml', 'yml', 'properties', 'conf', 'cfg', 'ini', 'toml', 'prop'];
      if (configTypes.includes(extension)) return 'config';
      
      // 脚本文件类型
      const scriptTypes = ['sh', 'bash', 'zsh', 'bat', 'cmd', 'ps1'];
      if (scriptTypes.includes(extension)) return 'script';
      
      // 日志文件类型
      const logTypes = ['log', 'out', 'err', 'trace'];
      if (logTypes.includes(extension)) return 'log';
      
      // 数据文件类型
      const dataTypes = ['csv', 'tsv', 'xlsx', 'xls'];
      if (dataTypes.includes(extension)) return 'data';
      
      // 文本文件类型
      const textTypes = ['txt', 'md', 'markdown', 'rst'];
      if (textTypes.includes(extension)) return 'text';
      
      // 压缩文件类型
      const archiveTypes = ['zip', 'tar', 'gz', 'bz2', 'rar', '7z'];
      if (archiveTypes.includes(extension)) return 'archive';
      
      // 特殊处理XML文件
      if (extension === 'xml') return 'xml';
      
      // 特殊处理JSON文件
      if (extension === 'json') return 'json';
      
      // 特殊处理YAML文件
      if (extension === 'yaml' || extension === 'yml') return 'yaml';
      
      // 特殊处理Properties文件
      if (extension === 'properties' || extension === 'prop') return 'properties';
      
      // 特殊处理Shell脚本
      if (scriptTypes.includes(extension)) return 'shell';
      
      // 处理没有扩展名的特殊文件名
      if (!extension) {
        const specialFiles = {
          'options': 'config',
          'config': 'config',
          'dockerfile': 'docker',
          'makefile': 'script',
          'readme': 'text'
        };
        
        const lowerFileName = this.fileName.toLowerCase();
        return specialFiles[lowerFileName] || 'default';
      }
      
      return 'default';
    },
    
    iconClass() {
      return {
        [`file-icon-${this.fileType}`]: true,
        'with-extension': this.showExtension && this.fileExtension
      };
    },
    
    iconStyle() {
      return {
        width: `${this.size}px`,
        height: `${this.size}px`,
        fontSize: `${Math.max(9, Math.floor(this.size / 3))}px`
      };
    }
  }
};
</script>

<style scoped>
.file-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  position: relative;
  border-radius: 4px;
  color: white;
  font-size: 10px;
  margin-right: 8px;
}

.file-extension {
  font-size: 0.6em;
  font-weight: bold;
  text-transform: uppercase;
  position: absolute;
  bottom: 3px;
  letter-spacing: -0.5px;
}

/* 默认文件图标 */
.file-icon-default {
  background: linear-gradient(135deg, #A0AEC0 0%, #718096 100%);
}

/* 配置文件图标 */
.file-icon-config {
  background: linear-gradient(135deg, #4299E1 0%, #3182CE 100%);
}

/* XML文件图标 */
.file-icon-xml {
  background: linear-gradient(135deg, #F6AD55 0%, #ED8936 100%);
}

/* JSON文件图标 */
.file-icon-json {
  background: linear-gradient(135deg, #FC8181 0%, #F56565 100%);
}

/* YAML文件图标 */
.file-icon-yaml {
  background: linear-gradient(135deg, #4FD1C5 0%, #38B2AC 100%);
}

/* Properties文件图标 */
.file-icon-properties {
  background: linear-gradient(135deg, #9F7AEA 0%, #805AD5 100%);
}

/* 脚本文件图标 */
.file-icon-script, .file-icon-shell {
  background: linear-gradient(135deg, #48BB78 0%, #38A169 100%);
}

/* 日志文件图标 */
.file-icon-log {
  background: linear-gradient(135deg, #A0AEC0 0%, #718096 100%);
}

/* 数据文件图标 */
.file-icon-data {
  background: linear-gradient(135deg, #63B3ED 0%, #4299E1 100%);
}

/* 文本文件图标 */
.file-icon-text {
  background: linear-gradient(135deg, #CBD5E0 0%, #A0AEC0 100%);
}

/* 压缩文件图标 */
.file-icon-archive {
  background: linear-gradient(135deg, #F6AD55 0%, #DD6B20 100%);
}

/* Docker文件图标 */
.file-icon-docker {
  background: linear-gradient(135deg, #2B6CB0 0%, #2C5282 100%);
}

/* 为小尺寸图标调整样式 */
@media (max-width: 768px) {
  .file-icon {
    margin-right: 4px;
  }
}
</style> 