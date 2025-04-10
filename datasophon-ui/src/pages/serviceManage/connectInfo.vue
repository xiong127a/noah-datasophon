<!--
/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */


 * @describe: 连接信息组件
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
          <div class="info-cards">
            <div 
              v-for="item in basicInfoArray" 
              :key="item.label" 
              class="info-card"
            >
              <div class="info-card-content">
                <div class="info-label">{{ item.label }}</div>
                <div class="info-value" @click="copyText(item.value)" :title="'点击复制: ' + item.value">
                  <span>{{ item.value }}</span>
                  <a-tooltip title="复制">
                    <a-icon
                      type="copy"
                      class="action-icon copy-icon"
                      @click.stop="copyText(item.value)"
                    />
                  </a-tooltip>
                </div>
              </div>
            </div>
            
            <!-- JDBC URL信息 -->
            <div 
              v-for="(jdbc, index) in jdbcUrlArray" 
              :key="'jdbc-' + index" 
              class="info-card jdbc-card-info"
            >
              <div class="info-card-content">
                <div class="info-label">{{ jdbc.label }}</div>
                <div class="info-value" @click="copyText(jdbc.value)" :title="'点击复制: ' + jdbc.value">
                  <span class="jdbc-link">{{ jdbc.value }}</span>
                  <a-tooltip title="复制URL">
                    <a-icon
                      type="copy"
                      class="action-icon copy-icon"
                      @click.stop="copyText(jdbc.value)"
                    />
                  </a-tooltip>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Java 示例代码 -->
        <div v-else-if="activeTab === 1" class="info-panel">
          <div class="code-card">
            <div class="code-header">
              <span class="code-title">Java连接示例</span>
              <a-tooltip title="复制代码">
                <a-icon
                  type="copy"
                  class="action-icon"
                  @click="copyText(connectionInfo.javaCode)"
                />
              </a-tooltip>
            </div>
            <div class="code-content">
              <div class="title-bar">
                <div class="file-name">Connection.java</div>
              </div>
              <div ref="javaCodeBlock" class="code-block language-java">{{ connectionInfo.javaCode }}</div>
              <div class="status-bar">
                <div class="status-item encoding">UTF-8</div>
                <div class="status-item">LF</div>
                <div class="status-item filetype">Java</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Python 示例代码 -->
        <div v-else-if="activeTab === 2" class="info-panel">
          <div class="code-card">
            <div class="code-header">
              <span class="code-title">Python连接示例</span>
              <a-tooltip title="复制代码">
                <a-icon
                  type="copy"
                  class="action-icon"
                  @click="copyText(connectionInfo.pythonCode)"
                />
              </a-tooltip>
            </div>
            <div class="code-content">
              <div class="title-bar">
                <div class="file-name">hive_connection.py</div>
              </div>
              <div ref="pythonCodeBlock" class="code-block language-python">{{ connectionInfo.pythonCode }}</div>
              <div class="status-bar">
                <div class="status-item encoding">UTF-8</div>
                <div class="status-item">LF</div>
                <div class="status-item filetype">Python</div>
              </div>
            </div>
          </div>
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
                  <span class="prompt">[root@bdp1 ~]#</span>
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
export default {
  name: "ConnectInfo",
  props: {
    serviceId: {
      type: [String, Number],
      required: true
    }
  },
  data() {
    return {
      loading: false,
      connectionInfo: null,
      activeTab: 0,
      tabs: [
        { title: '基本信息' },
        { title: 'Java代码' },
        { title: 'Python代码' },
        { title: '命令行' }
      ],
      // 临时缓存高亮处理后的代码
      highlightedCode: {
        java: null,
        python: null
      }
    };
  },
  computed: {
    isEmpty() {
      if (!this.connectionInfo) return true;
      
      // 检查是否所有关键字段都为空
      const { basicInfo, jdbcUrl, javaCode, pythonCode, beelineCommand } = this.connectionInfo;
      return (!basicInfo || Object.keys(basicInfo).length === 0) &&
             !jdbcUrl && !javaCode && !pythonCode && !beelineCommand;
    },
    basicInfoArray() {
      if (!this.connectionInfo || !this.connectionInfo.basicInfo) return [];
      
      // 将basicInfo对象转换为数组格式，适配列表展示
      return Object.entries(this.connectionInfo.basicInfo).map(([label, value]) => ({
        label,
        value: value || '-'
      }));
    },
    jdbcUrlArray() {
      if (!this.connectionInfo) return [];
      
      if (this.connectionInfo.jdbcUrls && this.connectionInfo.jdbcUrls.length > 0) {
        return this.connectionInfo.jdbcUrls;
      } else if (this.connectionInfo.jdbcUrl) {
        return [{
          label: 'JDBC URL',
          value: this.connectionInfo.jdbcUrl
        }];
      }
      
      return [];
    },
    commandLineArray() {
      if (!this.connectionInfo) return [];
      
      if (this.connectionInfo.commandLines && this.connectionInfo.commandLines.length > 0) {
        return this.connectionInfo.commandLines;
      } else if (this.connectionInfo.beelineCommand) {
        return [{
          label: 'Beeline 命令',
          value: this.connectionInfo.beelineCommand
        }];
      }
      
      return [];
    }
  },
  created() {
    console.log("ConnectInfo组件被创建，serviceId:", this.serviceId);
  },
  mounted() {
    console.log("ConnectInfo组件已挂载，serviceId:", this.serviceId);
    this.getConnectionInfo();
    this.loadHighlightjs();
  },
  updated() {
    this.highlightCode();
  },
  methods: {
    getConnectionInfo() {
      const serviceInstanceId = this.serviceId;
      
      if (!serviceInstanceId) {
        console.error("缺少serviceId参数，无法获取连接信息");
        this.$message.warning("服务ID未设置，无法获取连接信息");
        return;
      }
      
      console.log("开始获取连接信息，serviceInstanceId:", serviceInstanceId);
      this.loading = true;
      
      this.$axiosPost(global.API.getConnectionInfo, {
        serviceInstanceId: serviceInstanceId
      })
        .then((res) => {
          console.log("连接信息响应:", res);
          
          if (res.code === 200) {
            this.connectionInfo = res.data || {};
            console.log("获取到的连接信息:", this.connectionInfo);
            
            // 获取数据后触发代码高亮
            this.$nextTick(() => {
              this.highlightCode();
            });
          } else {
            this.$message.error(res.msg || "获取连接信息失败");
            this.connectionInfo = null;
          }
        })
        .catch((err) => {
          console.error("获取连接信息失败:", err);
          this.$message.error("连接服务器失败");
          this.connectionInfo = null;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    copyText(text) {
      if (!text) return;
      
      const textarea = document.createElement("textarea");
      textarea.value = text;
      document.body.appendChild(textarea);
      textarea.select();
      
      try {
        document.execCommand("copy");
        this.$message.success("复制成功");
      } catch (err) {
        this.$message.error("复制失败");
      } finally {
        document.body.removeChild(textarea);
      }
    },
    // 加载highlight.js
    loadHighlightjs() {
      // 在这里我们仅使用CSS来伪造一个代码高亮效果
      // 真实场景下，应该引入highlight.js或prismjs等库
      console.log("准备代码高亮环境");
    },
    // 对代码进行高亮处理
    highlightCode() {
      if (!this.connectionInfo) return;
      
      // 修正标签页索引，Java代码现在是第1个标签页（索引1）
      if (this.activeTab === 1 && this.$refs.javaCodeBlock) {
        console.log("应用Java代码高亮");
        const javaCode = this.connectionInfo.javaCode || '';
        
        try {
          // 创建专业IDE风格的代码块
          let lines = javaCode.split('\n');
          let formattedCode = '';
          
          // 为每行代码添加行号和包装
          lines.forEach((line, index) => {
            // 清理行，确保不包含现有标记
            const cleanLine = line.replace(/<\/?span[^>]*>/g, '');
            
            // 创建临时容器以进行处理，避免标签嵌套问题
            let tempContent = cleanLine;
            const placeholders = [];
            let placeholderIndex = 0;
            
            // 1. 先处理字符串，避免关键字错误匹配
            tempContent = tempContent.replace(/(["'])(?:(?=(\\?))\2.)*?\1/g, (match) => {
              const placeholder = `___PLACEHOLDER_${placeholderIndex}___`;
              placeholders.push(`<span class="string">${match}</span>`);
              placeholderIndex++;
              return placeholder;
            });
            
            // 2. 处理注释
            tempContent = tempContent.replace(/\/\/.*$/g, (match) => {
              const placeholder = `___PLACEHOLDER_${placeholderIndex}___`;
              placeholders.push(`<span class="comment">${match}</span>`);
              placeholderIndex++;
              return placeholder;
            });
            
            // 3. 处理数字
            tempContent = tempContent.replace(/\b(\d+)\b/g, (match) => {
              const placeholder = `___PLACEHOLDER_${placeholderIndex}___`;
              placeholders.push(`<span class="number">${match}</span>`);
              placeholderIndex++;
              return placeholder;
            });
            
            // 4. 处理关键字，确保不影响已处理内容
            tempContent = tempContent.replace(/\b(public|private|protected|class|interface|enum|extends|implements|new|if|else|for|while|do|switch|case|break|continue|return|try|catch|finally|throw|throws|static|final|void|abstract|instanceof|super|this|package|import|true|false|null)\b/g, (match) => {
              const placeholder = `___PLACEHOLDER_${placeholderIndex}___`;
              placeholders.push(`<span class="keyword">${match}</span>`);
              placeholderIndex++;
              return placeholder;
            });
            
            // 5. 恢复所有占位符
            for (let i = 0; i < placeholders.length; i++) {
              tempContent = tempContent.replace(`___PLACEHOLDER_${i}___`, placeholders[i]);
            }
            
            // 添加行包装
            formattedCode += `<span>${tempContent}</span>`;
          });
          
          // 如果DOM元素存在，更新内容
          if (this.$refs.javaCodeBlock) {
            this.$refs.javaCodeBlock.innerHTML = formattedCode;
          }
        } catch (error) {
          console.error('Java代码高亮处理错误:', error);
          // 出错时显示原始代码
          if (this.$refs.javaCodeBlock) {
            this.$refs.javaCodeBlock.textContent = javaCode;
          }
        }
      }
      
      // 修正标签页索引，Python代码现在是第2个标签页（索引2）
      if (this.activeTab === 2 && this.$refs.pythonCodeBlock) {
        console.log("应用Python代码高亮");
        const pythonCode = this.connectionInfo.pythonCode || '';
        
        try {
          // 分割成行来处理
          const lines = pythonCode.split('\n');
          let formattedCode = '';
          
          // 为每行代码添加高亮和行号
          lines.forEach((line, index) => {
            // 清理行，确保不包含现有标记
            const cleanLine = line.replace(/<\/?span[^>]*>/g, '');
            
            // 应用高级高亮，分步处理避免嵌套标签问题
            let processedLine = cleanLine;
            
            // 1. 先处理字符串，避免关键字错误匹配
            processedLine = processedLine.replace(/(["'])(?:(?=(\\?))\2.)*?\1/g, '<span class="string">$&</span>');
            
            // 2. 处理注释
            processedLine = processedLine.replace(/#.*$/g, '<span class="comment">$&</span>');
            
            // 3. 处理数字
            processedLine = processedLine.replace(/\b(\d+)\b/g, '<span class="number">$1</span>');
            
            // 4. 最后处理关键字，避免匹配字符串或注释中的关键字
            // 正则表达式中使用负向前瞻来避免匹配已经标记的内容
            const keywordRegex = new RegExp('\\b(def|class|if|elif|else|for|while|try|except|finally|with|as|import|from|return|break|continue|pass|in|is|not|and|or|True|False|None)\\b', 'g');
            
            // 临时替换已标记内容以避免重复处理
            let tempContent = processedLine;
            const placeholders = [];
            let placeholderIndex = 0;
            
            // 临时替换所有标签内容
            tempContent = tempContent.replace(/<span class="[^"]+">[\s\S]*?<\/span>/g, (match) => {
              const placeholder = `___PLACEHOLDER_${placeholderIndex}___`;
              placeholders.push(match);
              placeholderIndex++;
              return placeholder;
            });
            
            // 处理关键字
            tempContent = tempContent.replace(keywordRegex, '<span class="keyword">$1</span>');
            
            // 恢复占位符
            for (let i = 0; i < placeholders.length; i++) {
              tempContent = tempContent.replace(`___PLACEHOLDER_${i}___`, placeholders[i]);
            }
            
            // 添加行包装
            formattedCode += `<span>${tempContent}</span>`;
          });
          
          // 如果DOM元素存在，更新内容
          if (this.$refs.pythonCodeBlock) {
            this.$refs.pythonCodeBlock.innerHTML = formattedCode;
          }
        } catch (error) {
          console.error('Python代码高亮处理错误:', error);
          // 出错时显示原始代码
          if (this.$refs.pythonCodeBlock) {
            this.$refs.pythonCodeBlock.textContent = pythonCode;
          }
        }
      }
    }
  },
  watch: {
    serviceId: {
      handler(newVal) {
        if (newVal) {
          console.log("serviceId变更为:", newVal);
          this.getConnectionInfo();
        }
      },
      immediate: true
    },
    activeTab() {
      // 标签页切换时触发代码高亮
      this.$nextTick(() => {
        this.highlightCode();
      });
    }
  }
};
</script>

<style lang="less" scoped>
// ==== 全局变量 ====
@primary-color: #0071e3;
@success-color: #34c759;
@warning-color: #ff9500;
@error-color: #ff3b30;
@bg-color: #ffffff;
@bg-color-secondary: #f5f5f7;
@bg-color-tertiary: #fafafa;
@text-color: #1d1d1f;
@text-color-secondary: #86868b;
@border-color: #d2d2d7;
@font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", Helvetica, Arial, sans-serif;
@code-font-family: "SF Mono", SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", Courier, monospace;
@border-radius: 12px;
@container-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
@anim-duration: 0.3s;
@card-padding: 20px;

// 苹果风格配色 - 浅色彩虹渐变
@rainbow-gradient: linear-gradient(135deg, #FF9BB3 0%, #FFCC80 20%, #FFF59D 40%, #A5D6A7 60%, #90CAF9 80%, #CE93D8 100%);
@gradient-primary: linear-gradient(135deg, #90CAF9 0%, #B39DDB 100%);
@gradient-success: linear-gradient(135deg, #A5D6A7 0%, #DCEDC8 100%);
@gradient-header: linear-gradient(135deg, #B39DDB 0%, #9FA8DA 50%, #90CAF9 100%);
@gradient-card: linear-gradient(135deg, #FFFFFF 0%, #F5F7FA 100%);
@gradient-code-bg: linear-gradient(160deg, #263238 0%, #37474F 100%);
@active-tab-gradient: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
@glow-shadow: 0 0 20px rgba(94, 92, 230, 0.4);
@tab-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

// IDE主题
@ide-bg: #1e1e1e;
@ide-toolbar: #333333;
@ide-text: #e0e0e0;
@ide-line-number: #6e7681;
@ide-selection: rgba(0, 113, 227, 0.3);
@ide-cursor: #0063e1;

// ==== 基础布局样式 ====
.connection-wrapper {
  font-family: @font-family;
  color: @text-color;
  background-color: @bg-color;
  border-radius: @border-radius;
  box-shadow: @container-shadow;
  padding: 24px;
  min-height: 500px;
  position: relative;
  overflow: hidden;
  background-image: linear-gradient(to bottom, #ffffff, #f8f9ff);
  border: 1px solid rgba(210, 210, 215, 0.5);
}

.connection-header {
  margin-bottom: 20px;
  background: @gradient-header;
  margin: -24px -24px 20px -24px;
  padding: 24px;
  border-radius: @border-radius @border-radius 0 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 1px 20px rgba(0, 0, 0, 0.15);
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: -10px;
    left: -10px;
    right: -10px;
    bottom: -10px;
    background: @rainbow-gradient;
    opacity: 0.15;
    filter: blur(15px);
    z-index: 0;
  }
  
  .title {
    font-size: 28px;
    font-weight: 600;
    color: white;
    margin: 0;
    line-height: 1.2;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
    background: linear-gradient(90deg, #fff 0%, #e0e0e0 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    animation: titleGlow 3s infinite alternate;
    position: relative;
    z-index: 1;
  }
}

@keyframes titleGlow {
  0% {
    text-shadow: 0 0 5px rgba(255, 255, 255, 0.1);
  }
  100% {
    text-shadow: 0 0 15px rgba(255, 255, 255, 0.4);
  }
}

// ==== 加载和空状态 ====
.loading-container, .empty-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .loading-icon {
    font-size: 32px;
    color: @primary-color;
    margin-bottom: 16px;
    animation: pulse 1.5s infinite ease-in-out;
  }
  
  .loading-text {
    color: @text-color-secondary;
    font-size: 16px;
    background: @gradient-primary;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    font-weight: 500;
  }
}

@keyframes pulse {
  0% { transform: scale(0.95); opacity: 0.7; }
  50% { transform: scale(1.05); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.7; }
}

// ==== 分段控制器 (标签页) ====
.segment-control {
  display: flex;
  background-color: rgba(245, 245, 247, 0.8);
  border-radius: 20px;
  padding: 3px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  position: relative;
  z-index: 1;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: @rainbow-gradient;
    opacity: 0.1;
    z-index: -1;
    filter: blur(5px);
  }
  
  .segment-item {
    flex: 1;
    text-align: center;
    padding: 12px 8px;
    font-size: 14px;
    font-weight: 500;
    color: @text-color-secondary;
    cursor: pointer;
    transition: all @anim-duration ease;
    border-radius: 17px;
    user-select: none;
    white-space: nowrap;
    position: relative;
    z-index: 2;
    
    &:hover:not(.active) {
      color: rgba(0, 0, 0, 0.8);
      background-color: rgba(255, 255, 255, 0.5);
    }
    
    &.active {
      background: @active-tab-gradient;
      color: #5E5CE6;
      box-shadow: @tab-shadow;
      font-weight: 600;
      
      &::after {
        content: '';
        position: absolute;
        bottom: -2px;
        left: 50%;
        transform: translateX(-50%);
        width: 30px;
        height: 2px;
        background: @rainbow-gradient;
        border-radius: 4px;
        animation: tabIndicator 0.3s ease forwards;
      }
    }
  }
}

@keyframes tabIndicator {
  0% { width: 0; opacity: 0; }
  100% { width: 30px; opacity: 1; }
}

// ==== 内容区域 ====
.content-area {
  position: relative;
  min-height: 300px;
}

.info-panel {
  animation: fadeIn @anim-duration ease;
  height: 100%;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

// ==== 基本信息表格 ====
.info-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.info-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 0 1px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: all 0.3s ease;
  position: relative;
  border: 1px solid rgba(210, 210, 215, 0.4);
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    border-color: rgba(94, 92, 230, 0.3);
    
    .info-card-content {
      background-color: rgba(245, 245, 247, 0.5);
    }
  }
  
  .info-card-content {
    display: flex;
    flex-direction: row;
    transition: background-color 0.3s ease;
  }
  
  .info-label {
    flex: 0 0 180px;
    font-size: 14px;
    font-weight: 600;
    color: #86868b;
    background: linear-gradient(to right, #f5f5f7 0%, #ffffff 100%);
    padding: 16px;
    border-right: 1px solid rgba(210, 210, 215, 0.5);
    display: flex;
    align-items: center;
  }
  
  .info-value {
    flex: 1;
    display: flex;
    align-items: center;
    padding: 16px;
    font-size: 15px;
    line-height: 1.4;
    color: @text-color;
    cursor: pointer;
    transition: background-color 0.2s ease;
    position: relative;
    overflow: hidden;
    
    &:hover {
      background-color: rgba(0, 0, 0, 0.02);
    }
    
    &:active {
      background-color: rgba(0, 0, 0, 0.05);
    }
    
    span {
      flex: 1;
      word-break: break-all;
      position: relative;
      
      &::after {
        content: ' (点击复制)';
        color: #5E5CE6;
        font-size: 12px;
        font-weight: 500;
        opacity: 0;
        display: inline-block;
        margin-left: 6px;
        transition: opacity 0.2s ease;
      }
    }
    
    &:hover span::after {
      opacity: 0.7;
    }
    
    .copy-icon {
      opacity: 0;
      transition: opacity 0.2s ease, transform 0.2s ease;
      transform: scale(0.9);
      margin-left: 8px;
      z-index: 2;
    }
  }
  
  &:hover .copy-icon {
    opacity: 1;
    transform: scale(1);
  }
}

.jdbc-card-info {
  background: linear-gradient(135deg, rgba(94, 92, 230, 0.04) 0%, rgba(94, 92, 230, 0.01) 100%);
  border: 1px solid rgba(94, 92, 230, 0.15);
  
  .info-label {
    color: #5E5CE6;
    background: linear-gradient(to right, rgba(94, 92, 230, 0.08) 0%, rgba(94, 92, 230, 0.02) 100%);
    border-right: 1px solid rgba(94, 92, 230, 0.15);
  }
  
  .info-value .jdbc-link {
    color: #0071e3;
    font-family: @code-font-family;
    cursor: pointer;
    transition: all 0.2s ease;
    font-weight: 500;
    font-size: 14px;
    line-height: 1.5;
    display: inline-block;
    
    &:hover {
      color: #0077ED;
      text-decoration: underline;
    }
  }
}

.copy-icon {
  color: #5E5CE6;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 6px;
  background-color: transparent;
  border-radius: 50%;
  
  &:hover {
    background-color: rgba(94, 92, 230, 0.1);
    color: #5E5CE6;
    transform: scale(1.1);
  }
}

// ==== 代码卡片 ====
.code-card {
  background-color: @bg-color;
  border-radius: @border-radius;
  border: 1px solid @border-color;
  overflow: hidden;
  transition: all @anim-duration ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  margin-bottom: 16px;
  
  &:hover {
    box-shadow: @glow-shadow;
    transform: translateY(-2px);
  }
  
  .code-header {
    background: @gradient-header;
    padding: 12px 16px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .code-title {
      font-weight: 500;
      color: white;
      text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
    }
  }
  
  .code-content {
    padding: 0;
    max-height: 600px;
    overflow: auto;
    background: @ide-bg;
    position: relative;
    
    /* 顶部工具栏 */
    &::before {
      content: "";
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 30px;
      background: @ide-toolbar;
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
      background: @gradient-header;
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
    
    .code-block {
      margin: 0;
      padding: 45px 16px 22px;  /* 为底部状态栏留出空间 */
      font-family: @code-font-family;
      font-size: 13px;
      white-space: pre-wrap;
      word-break: break-all;
      color: @ide-text;
      line-height: 1.5;
      tab-size: 4;
      background: @gradient-code-bg;
      
      /* 添加左侧行号/边栏区域 */
      &.language-java, 
      &.language-python,
      &.language-sql,
      &.language-bash {
        &::before {
          content: "";
          position: absolute;
          left: 0;
          top: 45px;
          bottom: 22px;
          width: 40px;
          background: rgba(30, 30, 30, 0.7);
          border-right: 1px solid rgba(100, 100, 100, 0.3);
        }
      }
    }
  }
}

// ==== 操作图标 ====
.action-icon {
  color: #ffffff;
  font-size: 16px;
  cursor: pointer;
  transition: all @anim-duration ease;
  padding: 6px;
  opacity: 0.9;
  background-color: rgba(94, 92, 230, 0.7);
  border-radius: 50%;
  box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);
  
  &:hover {
    color: #ffffff;
    background: @rainbow-gradient;
    transform: scale(1.1);
    opacity: 1;
    box-shadow: 0 0 10px rgba(94, 92, 230, 0.6);
  }
}

// 微调标题区域的复制按钮
.code-header .action-icon,
.jdbc-header .action-icon,
.command-header .action-icon {
  margin-left: 8px;
}

/* 光标闪烁效果 */
.code-block::before {
  display: none !important; /* 完全禁用光标效果 */
}

.code-card:hover .code-block::before {
  display: none !important; /* 悬停时也禁用光标效果 */
}

/* 增加IDE风格的文件路径显示 */
.code-card .code-content::before {
  content: "terminal";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 30px;
  font-family: "SF Mono", SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", Courier, monospace;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  text-align: center;
  line-height: 30px;
  text-transform: lowercase;
  z-index: 3;
}

/* 隐藏重复的状态栏信息 */
.code-card .code-content::after,
.jdbc-card .jdbc-content::after,
.command-card .command-content::after {
  display: none;
}

/* 恢复命令行光标闪烁效果 */
@keyframes cursor-blink {
  0%, 100% { opacity: 0; }
  50% { opacity: 1; }
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

/* 代码块光标禁用 - 确保不影响命令行光标 */
.code-block::before {
  display: none !important; /* 完全禁用代码块编辑器光标效果 */
}

.code-card:hover .code-block::before {
  display: none !important; /* 悬停时也禁用代码块编辑器光标效果 */
}

/* 增加IDE风格的文件路径显示 */
.code-card .code-content::before {
  content: "terminal";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 30px;
  font-family: "SF Mono", SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", Courier, monospace;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  text-align: center;
  line-height: 30px;
  text-transform: lowercase;
  z-index: 3;
}

/* 为VS Code风格添加底部状态栏 */
.code-card .code-content::after {
  display: none; /* 移除重复的状态信息 */
}

/* 调整JDBC/命令行状态栏样式，确保不会出现重复信息 */
.jdbc-card .jdbc-content::after,
.command-card .command-content::after {
  display: none;
}

// 添加SQL和Bash终端样式
.terminal-content {
  margin: 0;
  padding: 45px 16px 22px;  /* 为底部状态栏留出空间 */
  font-family: @code-font-family;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
  color: @ide-text;
  line-height: 1.5;
  tab-size: 4;
  background: @gradient-code-bg;
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

/* 恢复命令行光标闪烁效果 */
@keyframes cursor-blink {
  0%, 100% { opacity: 0; }
  50% { opacity: 1; }
}

// SQL终端特定样式
.sql-terminal {
  background: linear-gradient(160deg, #1a237e 0%, #283593 100%);
  border-left: 4px solid #3949ab;
  
  .terminal-line {
    .prompt {
      color: #64b5f6;
    }
    
    .command {
      color: #e0e0e0;
    }
  }
  
  .terminal-cursor {
    background-color: #64b5f6;
  }
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

// 修改JDBC和命令行卡片内容样式
.jdbc-content .code-block,
.command-content .code-block {
  display: none; // 隐藏原来的代码块
}

// 添加终端顶部图标和窗口控制
.jdbc-content::before,
.command-content::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 30px;
  background: #424242;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  z-index: 1;
}

// 添加终端的顶部图标
.sql-terminal::after {
  content: "●●●";
  position: absolute;
  top: 8px;
  left: 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  line-height: 1;
  letter-spacing: 2px;
  z-index: 2;
}

.bash-terminal::after {
  content: "";  /* 移除了终端左上角的三个点 */
}

// 添加数据库连接风格样式
.connection-terminal {
  background: linear-gradient(160deg, #263238 0%, #37474F 100%);
  padding: 55px 16px 22px !important;
  
  .url-bar {
    display: flex;
    background-color: rgba(255, 255, 255, 0.08);
    border-radius: 6px;
    padding: 8px 12px;
    align-items: center;
    border: 1px solid rgba(255, 255, 255, 0.12);
    
    .url-protocol {
      color: #64b5f6;
      font-weight: 500;
      margin-right: 4px;
      flex-shrink: 0;
    }
    
    .url-value {
      color: #e0e0e0;
      font-size: 14px;
      word-break: break-all;
      font-family: @code-font-family;
      flex: 1;
    }
    
    .url-action {
      color: rgba(255, 255, 255, 0.6);
      font-size: 14px;
      margin-left: 8px;
      cursor: pointer;
      
      &:hover {
        color: rgba(255, 255, 255, 0.9);
      }
    }
  }
  
  &::after {
    display: none;
  }
}

.info-row {
  &.jdbc-row {
    background-color: rgba(94, 92, 230, 0.05);
    
    .info-label {
      font-weight: 600;
      color: #5E5CE6;
    }
    
    .info-value {
      .jdbc-link {
        color: #0071e3;
        font-family: @code-font-family;
        cursor: pointer;
        transition: all 0.2s ease;
        font-weight: 500;
        font-size: 14px;
        line-height: 1.5;
        padding: 3px 6px;
        border-radius: 4px;
        background-color: rgba(0, 113, 227, 0.08);
        border: 1px solid rgba(0, 113, 227, 0.15);
        display: inline-block;
        
        &:hover {
          color: #0077ED;
          text-decoration: underline;
          background-color: rgba(0, 113, 227, 0.12);
          border-color: rgba(0, 113, 227, 0.25);
        }
      }
    }
  }
}

/* 代码块光标禁用 - 确保不影响命令行光标 */
.code-block::before {
  display: none !important; /* 完全禁用代码块编辑器光标效果 */
}

.code-card:hover .code-block::before {
  display: none !important; /* 悬停时也禁用代码块编辑器光标效果 */
}

// ==== 命令行和JDBC卡片 ====
.jdbc-card, .command-card {
  background: @gradient-card;
  border-radius: @border-radius;
  border: 1px solid @border-color;
  overflow: hidden;
  margin-bottom: 16px;
  transition: all @anim-duration ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  position: relative;
  
  &:hover {
    box-shadow: @glow-shadow;
    transform: translateY(-2px);
    
    &::before {
      opacity: 0.3;
    }
  }
  
  &::before {
    content: '';
    position: absolute;
    top: -2px;
    left: -2px;
    right: -2px;
    bottom: -2px;
    background: @rainbow-gradient;
    background-size: 200% 200%;
    z-index: -1;
    filter: blur(8px);
    opacity: 0;
    transition: opacity 0.3s ease;
    border-radius: @border-radius;
  }
  
  .jdbc-header, .command-header {
    background: @gradient-header;
    padding: 12px 16px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .jdbc-title, .command-title {
      font-weight: 500;
      color: white;
      text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
    }
  }
  
  .jdbc-content, .command-content {
    padding: 0;
    background: @ide-bg;
    position: relative;
    
    /* 顶部工具栏 */
    &::before {
      content: "";
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 30px;
      background: @ide-toolbar;
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
      background: @gradient-header;
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
</style>

<!-- 添加非Scoped样式以支持代码高亮 -->
<style>
/* IDE风格设计 - 代码高亮部分 */
.code-block {
  position: relative;
  overflow: hidden;
}

/* IDE行号 */
.code-block.language-java > span,
.code-block.language-python > span {
  position: relative;
  display: block;
  padding-left: 50px;
  counter-increment: line;
}

.code-block.language-java > span::before,
.code-block.language-python > span::before {
  content: counter(line);
  position: absolute;
  left: 0;
  width: 40px;
  text-align: right;
  color: #6e7681;
  font-size: 12px;
  padding-right: 10px;
  user-select: none;
}

/* Java代码高亮 - Pro主题风格 */
.language-java .keyword,
.language-java .import,
.language-java .package,
.language-java .class,
.language-java .public,
.language-java .private,
.language-java .protected,
.language-java .static,
.language-java .final,
.language-java .void,
.language-java .new,
.language-java .try,
.language-java .catch,
.language-java .throw,
.language-java .throws,
.language-java .if,
.language-java .else,
.language-java .for,
.language-java .while,
.language-java .do,
.language-java .switch,
.language-java .case,
.language-java .default,
.language-java .break,
.language-java .continue,
.language-java .return {
  color: #ff79c6;
  font-weight: bold;
  text-shadow: 0 0 2px rgba(255, 121, 198, 0.4);
}

.language-java .string,
.language-java .char {
  color: #f1fa8c;
  text-shadow: 0 0 2px rgba(241, 250, 140, 0.4);
}

.language-java .comment {
  color: #6272a4;
  font-style: italic;
}

.language-java .class-name {
  color: #8be9fd;
  text-shadow: 0 0 2px rgba(139, 233, 253, 0.4);
}

.language-java .function-name {
  color: #50fa7b;
  text-shadow: 0 0 2px rgba(80, 250, 123, 0.4);
}

.language-java .number {
  color: #bd93f9;
  text-shadow: 0 0 2px rgba(189, 147, 249, 0.4);
}

.language-java .boolean {
  color: #bd93f9;
  text-shadow: 0 0 2px rgba(189, 147, 249, 0.4);
}

/* Python代码高亮 - Pro主题风格 */
.language-python .keyword,
.language-python .def,
.language-python .class,
.language-python .if,
.language-python .else,
.language-python .elif,
.language-python .for,
.language-python .while,
.language-python .try,
.language-python .except,
.language-python .finally,
.language-python .import,
.language-python .from,
.language-python .as,
.language-python .pass,
.language-python .return,
.language-python .break,
.language-python .continue,
.language-python .in,
.language-python .is,
.language-python .not,
.language-python .and,
.language-python .or,
.language-python .with {
  color: #ff79c6;
  font-weight: bold;
  text-shadow: 0 0 2px rgba(255, 121, 198, 0.4);
}

.language-python .string,
.language-python .string-content {
  color: #f1fa8c;
  text-shadow: 0 0 2px rgba(241, 250, 140, 0.4);
}

.language-python .comment {
  color: #6272a4;
  font-style: italic;
}

.language-python .function-name {
  color: #50fa7b;
  text-shadow: 0 0 2px rgba(80, 250, 123, 0.4);
}

.language-python .class-name {
  color: #8be9fd;
  text-shadow: 0 0 2px rgba(139, 233, 253, 0.4);
}

.language-python .number {
  color: #bd93f9;
  text-shadow: 0 0 2px rgba(189, 147, 249, 0.4);
}

.language-python .boolean,
.language-python .none {
  color: #bd93f9;
  text-shadow: 0 0 2px rgba(189, 147, 249, 0.4);
}

/* 特殊效果 */
.code-block {
  position: relative;
  overflow: hidden;
}

/* 删除闪光效果 */
.code-block::after {
  display: none;
}

/* 增加IDE风格的文件路径显示 */
.code-card .code-content::before {
  content: "terminal";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 30px;
  font-family: "SF Mono", SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", Courier, monospace;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  text-align: center;
  line-height: 30px;
  text-transform: lowercase;
  z-index: 3;
}

/* 为VS Code风格添加底部状态栏 */
.code-card .code-content::after {
  display: none; /* 移除重复的状态信息 */
}

/* 调整JDBC/命令行状态栏样式，确保不会出现重复信息 */
.jdbc-card .jdbc-content::after,
.command-card .command-content::after {
  display: none;
}
</style> 