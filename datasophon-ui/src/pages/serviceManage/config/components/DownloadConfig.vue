<template>
  <div>
    <!-- 打包下载按钮组 -->
    <div class="download-actions">
      <a-dropdown :trigger="['click']" overlayClassName="download-dropdown">
        <a-button 
          type="primary" 
          class="download-all-btn"
          :loading="downloadLoading">
          <a-icon type="download" />
          打包下载
          <a-icon type="down" />
        </a-button>
        <a-menu slot="overlay">
          <a-menu-item v-for="formatInfo in formatsList" :key="formatInfo.format" @click="openDownloadModal(formatInfo.format)">
            <span class="file-menu-icon">
              <div class="file-type-icon file-icon-archive" style="width: 20px; height: 24px;"></div>
            </span>
            {{ formatInfo.format.toUpperCase() }}格式
          </a-menu-item>
        </a-menu>
      </a-dropdown>
    </div>

    <!-- 下载配置模态窗 -->
    <a-modal
      v-model="downloadModalVisible"
      title="下载配置文件"
      width="500px"
      @ok="downloadAllConfigs"
      okText="下载"
      cancelText="取消"
      :okButtonProps="{ loading: downloadLoading }"
      :destroyOnClose="true"
      class="download-modal"
    >
      <div class="download-options">
        <div class="option-row">
          <div class="option-label">压缩格式：</div>
          <div class="option-content">
            <a-tag class="format-tag">{{ selectedFormat.toUpperCase() }}</a-tag>
            <span class="format-desc">
              {{ getFormatDescription(selectedFormat) }}
            </span>
          </div>
        </div>
        
        <a-divider />
        
        <div class="option-row">
          <div class="option-label">密码保护：</div>
          <div class="option-content">
            <a-switch v-model="usePassword" @change="handlePasswordChange" :disabled="!supportPassword" />
            <span class="option-desc">{{ usePassword ? '开启' : (supportPassword ? '关闭' : '不支持') }}</span>
          </div>
        </div>
        
        <div v-if="usePassword" class="password-section">
          <a-form-model layout="vertical" :model="passwordForm" autocomplete="off">
            <a-form-model-item label="设置密码">
              <a-input-password 
                v-model="passwordForm.password"
                placeholder="请输入密码"
                autocomplete="new-password"
                :visibilityToggle="true"
                :name="'pwd_' + Date.now()"
                readonly
                @focus="removeReadonly"
              />
            </a-form-model-item>
            <a-form-model-item label="确认密码">
              <a-input-password 
                v-model="passwordForm.confirmPassword"
                placeholder="请再次输入密码"
                autocomplete="new-password"
                :visibilityToggle="true"
                :name="'confirm_pwd_' + Date.now()"
                readonly
                @focus="removeReadonly"
              />
              <div v-if="passwordError" class="password-error" role="alert">
                <a-icon type="warning" />
                {{ passwordError }}
              </div>
            </a-form-model-item>
          </a-form-model>
          
          <div class="password-tips" role="note">
            <a-icon type="info-circle" />
            密码保护提示：
            <ul>
              <li>压缩包将使用密码进行加密保护</li>
              <li>请妥善保管密码，密码丢失将无法解压文件</li>
              <li>不同压缩格式的加密强度可能不同</li>
            </ul>
          </div>
        </div>

        <!-- 打包进度显示 -->
        <div v-if="isCompressing" class="compress-progress" role="progressbar" :aria-valuenow="compressProgress" aria-valuemin="0" aria-valuemax="100">
          <div class="progress-header">
            <span class="progress-title">正在打包文件...</span>
            <span class="progress-percent">{{ compressProgress }}%</span>
          </div>
          <a-progress 
            :percent="compressProgress" 
            :showInfo="false"
            strokeColor="#007AFF"
            class="progress-bar"
          />
          <div class="progress-tips">
            <a-icon type="loading" />
            正在压缩文件，请稍候...
          </div>
        </div>
      </div>
    </a-modal>

    <!-- 进度模态窗 -->
    <a-modal
      v-model="progressModalVisible"
      :title="null"
      width="440px"
      :footer="null"
      :closable="false"
      :maskClosable="false"
      :destroyOnClose="true"
      class="progress-modal"
      :bodyStyle="{ padding: 0 }"
    >
      <div class="progress-content">
        <div class="progress-header">
          <div class="progress-icon">
            <a-icon type="loading" spin class="spinning-icon" />
          </div>
          <div class="progress-info">
            <h3>{{ progressTitle }}</h3>
            <p>{{ progressStatusText }}</p>
          </div>
        </div>
        
        <div class="progress-bar-container">
          <div class="progress-track">
            <div class="progress-fill" :style="{ transform: `translateX(${compressProgress - 100}%)` }">
              <div class="progress-glow"></div>
            </div>
          </div>
          <div class="progress-number">{{ compressProgress }}%</div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { downloadFile } from '@/utils/copyUtil';

export default {
  name: 'DownloadConfig',
  props: {
    serviceId: {
      type: [Number, String],
      required: true
    },
    serviceName: {
      type: String,
      required: true,
      default: '未知服务'
    }
  },
  data() {
    return {
      downloadLoading: false,
      downloadModalVisible: false,
      selectedFormat: 'zip',
      usePassword: false,
      passwordForm: {
        password: '',
        confirmPassword: ''
      },
      passwordError: '',
      isCompressing: false,
      compressProgress: 0,
      progressTimer: null,
      progressModalVisible: false,
      progressStartTime: null,
      progressTitle: '正在打包配置文件',
      progressStatusText: '正在准备文件...',
      minDisplayTime: 5000, // 最小显示时间（毫秒）
      simulatedProgress: 0, // 模拟进度
      realProgress: 0, // 实际进度
      simulateTimer: null, // 模拟进度的定时器
      formatsList: [],
    };
  },
  computed: {
    // 判断当前选择的格式是否支持密码
    supportPassword() {
      const formatInfo = this.formatsList.find(item => item.format === this.selectedFormat);
      return formatInfo && (formatInfo.supportPassword === '支持' || formatInfo.supportPassword === '需安装zip4j库');
    }
  },
  watch: {
    compressProgress(val) {
      if (val < 30) {
        this.progressStatusText = '正在准备文件...';
      } else if (val < 60) {
        this.progressStatusText = '正在压缩文件...';
      } else if (val < 90) {
        this.progressStatusText = '正在打包...';
      } else {
        this.progressStatusText = '即将完成...';
      }
    },
    // 监听格式变化，如果切换到不支持密码的格式，自动关闭密码保护
    selectedFormat(newFormat) {
      const formatInfo = this.formatsList.find(item => item.format === newFormat);
      if (formatInfo && formatInfo.supportPassword === '不支持' && this.usePassword) {
        this.usePassword = false;
        this.passwordForm = {
          password: '',
          confirmPassword: ''
        };
        this.passwordError = '';
      }
    }
  },
  mounted() {
    this.fetchSupportedCompressFormats();
  },
  methods: {
    // 获取支持的压缩格式列表
    async fetchSupportedCompressFormats() {
      try {
        if (!global.API || !global.API.getSupportedCompressFormats) {
          this.$message.error('系统配置错误，无法获取支持的压缩格式');
          // 使用默认值
          this.formatsList = [
            { format: 'zip', description: '兼容性最佳，几乎所有系统都支持', supportPassword: '需安装zip4j库' },
            { format: 'tar.gz', description: 'Linux/Unix系统常用格式，压缩率高', supportPassword: '不支持' },
            { format: '7z', description: '高压缩率，标准7z格式（不支持密码保护）', supportPassword: '不支持' }
          ];
          return;
        }
        
        const res = await this.$axiosGet(global.API.getSupportedCompressFormats);
        
        if (res.code === 200) {
          this.formatsList = res.data || [];
          console.log('获取到压缩格式列表：', this.formatsList);
        } else {
          this.$message.error(res.msg || '获取支持的压缩格式失败');
          // 使用默认值
          this.formatsList = [
            { format: 'zip', description: '兼容性最佳，几乎所有系统都支持', supportPassword: '需安装zip4j库' },
            { format: 'tar.gz', description: 'Linux/Unix系统常用格式，压缩率高', supportPassword: '不支持' },
            { format: '7z', description: '高压缩率，标准7z格式（不支持密码保护）', supportPassword: '不支持' }
          ];
        }
      } catch (error) {
        this.$message.error(`获取支持的压缩格式失败: ${error ? error.message || '未知错误' : '未知错误'}`);
        // 使用默认值
        this.formatsList = [
          { format: 'zip', description: '兼容性最佳，几乎所有系统都支持', supportPassword: '需安装zip4j库' },
          { format: 'tar.gz', description: 'Linux/Unix系统常用格式，压缩率高', supportPassword: '不支持' },
          { format: '7z', description: '高压缩率，标准7z格式（不支持密码保护）', supportPassword: '不支持' }
        ];
      }
    },

    // 打开下载模态窗
    openDownloadModal(format) {
      this.selectedFormat = format;
      this.usePassword = false;
      this.passwordForm = {
        password: '',
        confirmPassword: ''
      };
      this.passwordError = '';
      this.downloadModalVisible = true;
    },
    
    // 处理密码开关变化
    handlePasswordChange(checked) {
      if (!checked) {
        this.passwordForm = {
          password: '',
          confirmPassword: ''
        };
        this.passwordError = '';
      }
    },
    
    // 获取压缩格式的描述
    getFormatDescription(format) {
      // 从API返回的格式列表中查找对应格式的描述
      const formatInfo = this.formatsList.find(item => item.format === format);
      if (formatInfo) {
        return formatInfo.description;
      }
      
      // 如果找不到，则使用默认描述
      const descriptions = {
        'zip': '兼容性最佳，几乎所有系统都支持',
        'tar.gz': 'Linux/Unix系统常用格式，压缩率高',
        '7z': '高压缩率，支持多种加密方式',
        'tar': '无压缩的归档格式，用于打包多个文件',
        'tar.xz': 'Linux系统常用高压缩率格式',
        'gz': 'GZIP格式，单文件压缩，常用于Linux系统',
        'bz2': 'BZIP2格式，高压缩率，通常用于Linux系统'
      };
      return descriptions[format] || '压缩文件格式';
    },

    // 下载单个配置文件
    async downloadFile(fileName) {
      if (!fileName) {
        this.$message.error('文件名不能为空');
        return;
      }
      
      try {
        const downloadUrl = `${window.location.origin}${global.API.downloadServiceConfigFile}?serviceInstanceId=${this.serviceId}&fileName=${encodeURIComponent(fileName)}`;
        
        await downloadFile(downloadUrl, fileName, this);
      } catch (error) {
        this.$message.error('下载文件失败');
      }
    },
    
    // 向后兼容方法 - 被ConfigDownload.vue等组件通过ref调用
    downloadSingleConfig(record) {
      // 如果传入的是对象（如ConfigDownload组件传入的record），则提取fileName
      const fileName = record && typeof record === 'object' ? record.fileName : record;
      
      if (fileName) {
        return this.downloadFile(fileName);
      } else {
        this.$message.error('无效的文件信息');
        return Promise.reject(new Error('无效的文件信息'));
      }
    },

    // 开始监控打包进度
    startProgressMonitor() {
      this.isCompressing = true;
      this.compressProgress = 0;
      this.simulatedProgress = 0;
      this.realProgress = 0;
      this.progressModalVisible = true;
      this.progressStartTime = Date.now();
      
      // 启动模拟进度
      this.startSimulatedProgress();
      
      // 实际调用后端API获取进度
      this.progressTimer = setInterval(async () => {
        try {
          if (!global.API || !global.API.getCompressProgress) {
            this.$message.error('系统配置错误，无法获取压缩进度');
            return;
          }
          
          const res = await this.$axiosGet(
            `${global.API.getCompressProgress}?serviceInstanceId=${this.serviceId}`
          );
          
          if (res.code === 200 && res.data !== null) {
            this.realProgress = res.data;
            console.log('实际压缩进度:', this.realProgress);
            
            // 如果实际进度已完成
            if (this.realProgress >= 100) {
              // 快速完成剩余的模拟进度
              this.completeProgress();
            }
          } else {
            this.$message.error(res.msg || '获取压缩进度失败');
          }
        } catch (error) {
          console.error('获取打包进度失败:', error);
          this.$message.error('获取压缩进度失败');
        }
      }, 1000);
    },

    // 启动模拟进度
    startSimulatedProgress() {
      const totalSteps = 200; // 增加总步数，让进度条走得更慢
      const baseInterval = 75; // 增加间隔时间
      let step = 0;
      
      this.simulateTimer = setInterval(() => {
        if (this.simulatedProgress >= 100) {
          clearInterval(this.simulateTimer);
          return;
        }
        
        // 计算下一步进度
        step++;
        // 使用缓动函数使进度变化更自然
        const progress = this.easeProgress(step / totalSteps);
        // 限制模拟进度不超过实际进度
        this.simulatedProgress = Math.min(Math.floor(progress * 95), this.realProgress);
        // 更新显示的进度
        this.compressProgress = this.simulatedProgress;
        
        // 更新状态文本
        this.updateProgressStatus(this.simulatedProgress);
      }, baseInterval);
    },

    // 缓动函数，使进度变化更自然
    easeProgress(x) {
      return x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2;
    },

    // 快速完成剩余进度
    completeProgress() {
      clearInterval(this.simulateTimer);
      
      const remainingProgress = 100 - this.simulatedProgress;
      const steps = 10;
      const stepSize = remainingProgress / steps;
      let currentStep = 0;
      
      const finishTimer = setInterval(() => {
        currentStep++;
        this.simulatedProgress = Math.min(
          this.simulatedProgress + stepSize,
          100
        );
        this.compressProgress = Math.floor(this.simulatedProgress);
        
        if (currentStep >= steps) {
          clearInterval(finishTimer);
          // 确保最小显示时间
          const elapsedTime = Date.now() - this.progressStartTime;
          if (elapsedTime >= this.minDisplayTime) {
            this.stopProgressMonitor();
          } else {
            setTimeout(() => {
              this.stopProgressMonitor();
            }, this.minDisplayTime - elapsedTime);
          }
        }
      }, 100);
    },

    // 更新进度状态文本
    updateProgressStatus(progress) {
      if (progress < 20) {
        this.progressStatusText = '正在准备文件...';
      } else if (progress < 40) {
        this.progressStatusText = '正在分析文件结构...';
      } else if (progress < 60) {
        this.progressStatusText = '正在压缩文件...';
      } else if (progress < 80) {
        this.progressStatusText = '正在优化压缩...';
      } else if (progress < 95) {
        this.progressStatusText = '正在完成打包...';
      } else {
        this.progressStatusText = '即将完成...';
      }
    },

    // 停止进度监控
    stopProgressMonitor() {
      if (this.progressTimer) {
        clearInterval(this.progressTimer);
        this.progressTimer = null;
      }
      if (this.simulateTimer) {
        clearInterval(this.simulateTimer);
        this.simulateTimer = null;
      }
      
      this.isCompressing = false;
      this.progressModalVisible = false;
      this.compressProgress = 0;
      this.simulatedProgress = 0;
      this.realProgress = 0;
      this.progressStartTime = null;
    },

    // 打包下载所有配置文件
    async downloadAllConfigs() {
      // 验证密码
      if (this.usePassword) {
        if (!this.passwordForm.password) {
          this.passwordError = '请输入密码';
          return;
        }
        if (this.passwordForm.password !== this.passwordForm.confirmPassword) {
          this.passwordError = '两次输入的密码不一致';
          return;
        }
        if (this.passwordForm.password.length < 4) {
          this.passwordError = '密码长度不能少于4位';
          return;
        }
      }
      
      // 直接执行下载，不再弹出提示
      this.executeDownload();
    },
    
    // 执行下载操作
    executeDownload() {
      this.downloadLoading = true;
      
      try {
        if (!global.API || !global.API.downloadAllServiceConfigFiles) {
          this.$message.error('系统配置错误，无法下载文件');
          return;
        }
        
        // 先启动进度监控
        this.startProgressMonitor();
        
        // 构建下载URL，添加格式参数和可选的密码参数
        let downloadUrl = `${window.location.origin}${global.API.downloadAllServiceConfigFiles}?serviceInstanceId=${this.serviceId}&format=${this.selectedFormat}`;
        
        // 如果启用了密码保护，添加密码参数
        if (this.usePassword && this.passwordForm.password) {
          downloadUrl += `&password=${encodeURIComponent(this.passwordForm.password)}`;
        }
        
        // 根据不同格式设置不同的文件扩展名
        const fileExtension = this.selectedFormat === 'tar.gz' || this.selectedFormat === 'tar.xz' ? 
                             '.' + this.selectedFormat : `.${this.selectedFormat}`;
        const fileName = `${this.serviceName}_configs${fileExtension}`;
        
        // 使用通用下载工具函数替代直接操作DOM
        downloadFile(downloadUrl, fileName, this);
        
        this.$message.success(`正在下载 ${this.selectedFormat.toUpperCase()} 格式配置文件`);
        this.downloadModalVisible = false;
      } catch (error) {
        this.$message.error('下载配置文件失败');
        this.stopProgressMonitor();
      } finally {
        this.downloadLoading = false;
      }
    },

    // 移除密码输入框的readonly属性
    removeReadonly(e) {
      // 移除readonly属性以允许用户输入
      if (e && e.target) {
        e.target.removeAttribute('readonly');
      }
    }
  }
};
</script>

<style scoped>
.download-actions {
  display: flex;
}

.download-all-btn {
  border-radius: 12px;
  height: 42px;
  font-weight: 500;
  font-size: 15px;
  padding: 0 22px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border: none;
  transition: all 0.3s ease;
  background: linear-gradient(135deg, #007AFF 0%, #1F62EE 100%);
}

.download-all-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.download-all-btn:focus {
  box-shadow: 0 0 0 2px rgba(0, 122, 255, 0.3);
}

/* 下载模态窗样式 */
.download-modal /deep/ .ant-modal-content {
  border-radius: 16px;
  overflow: hidden;
}

.download-modal /deep/ .ant-modal-header {
  border-bottom: 1px solid #f0f0f0;
  padding: 20px 24px;
}

.download-modal /deep/ .ant-modal-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.download-modal /deep/ .ant-modal-footer {
  border-top: 1px solid #f0f0f0;
  padding: 16px 24px;
}

.download-modal /deep/ .ant-btn-primary {
  background: linear-gradient(135deg, #007AFF 0%, #1F62EE 100%);
  border: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.download-options {
  padding: 8px 0;
}

.option-row {
  display: flex;
  margin-bottom: 16px;
  align-items: center;
}

.option-label {
  width: 80px;
  font-weight: 500;
  color: #333;
}

.option-content {
  flex: 1;
  display: flex;
  align-items: center;
}

.format-tag {
  font-weight: 500;
  background-color: #e6f7ff;
  color: #007AFF;
  border-color: #91d5ff;
  margin-right: 12px;
}

.format-desc {
  color: #666;
  font-size: 14px;
}

.option-desc {
  margin-left: 12px;
  color: #666;
}

.password-section {
  background-color: #f9fafb;
  border-radius: 8px;
  padding: 16px;
  margin-top: 16px;
}

.password-error {
  color: #f5222d;
  font-size: 13px;
  margin-top: 4px;
}

.password-error .anticon {
  margin-right: 6px;
}

.password-tips {
  background-color: #fffbe6;
  border: 1px solid #ffe58f;
  padding: 12px 16px;
  border-radius: 8px;
  margin-top: 16px;
  color: #874d00;
  font-size: 13px;
}

.password-tips .anticon {
  color: #faad14;
  margin-right: 8px;
}

.password-tips ul {
  margin: 8px 0 0 0;
  padding-left: 24px;
  line-height: 1.6;
}

/* 打包进度显示 */
.compress-progress {
  margin-top: 16px;
  padding: 16px;
  background-color: #f9fafb;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.progress-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.progress-percent {
  font-size: 12px;
  color: #999;
}

.progress-bar {
  margin-top: 8px;
  margin-bottom: 8px;
}

.progress-tips {
  font-size: 12px;
  color: #666;
}

/* 进度模态窗样式 */
.progress-modal {
  width: 100%;
  max-width: 500px;
  padding: 0;
}

.progress-modal /deep/ .ant-modal-content {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

.progress-content {
  padding: 32px;
}

.progress-header {
  display: flex;
  align-items: center;
  margin-bottom: 28px;
}

.progress-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: linear-gradient(135deg, #007AFF 0%, #00C2FF 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.2);
}

.spinning-icon {
  font-size: 28px;
  color: #FFFFFF;
}

.progress-info {
  flex: 1;
}

.progress-info h3 {
  font-size: 18px;
  font-weight: 600;
  color: #000000;
  margin: 0 0 4px 0;
  line-height: 1.3;
}

.progress-info p {
  font-size: 14px;
  color: #666666;
  margin: 0;
  line-height: 1.5;
}

.progress-bar-container {
  position: relative;
  margin-top: 8px;
}

.progress-track {
  height: 6px;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
  overflow: hidden;
  position: relative;
}

.progress-fill {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, #007AFF 0%, #00C2FF 100%);
  transform: translateX(-100%);
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.progress-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.3) 50%,
    transparent 100%
  );
  animation: glow 2s linear infinite;
}

.progress-number {
  position: absolute;
  right: 0;
  top: -24px;
  font-size: 13px;
  font-weight: 500;
  color: #007AFF;
}

@keyframes glow {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

/* 菜单图标样式 */
.file-menu-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
  width: 18px;
  height: 22px;
}

/* 下拉菜单样式 */
.download-dropdown {
  min-width: 180px;
}

.download-dropdown /deep/ .ant-dropdown-menu-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
}

.download-dropdown /deep/ .ant-dropdown-menu-item:hover {
  background-color: #f5f9ff;
}

/* 文件图标样式 */
.file-type-icon {
  width: 32px;
  height: 40px;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  position: relative;
  filter: drop-shadow(0px 2px 3px rgba(0, 0, 0, 0.1));
  transition: all 0.3s ease;
}

.file-icon-archive {
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%23FF7043" d="M14,2H6C4.9,2,4,2.9,4,4v16c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2V8L14,2z"/><path fill="white" d="M14,3v5h5v11c0,0.6-0.4,1-1,1H6c-0.6,0-1-0.4-1-1V4c0-0.6,0.4-1,1-1H14z"/><path fill="%23FF7043" opacity="0.4" d="M14,2l6,6h-6V2z"/><path fill="%23FF7043" d="M11,14.5V16h2v-1.5h1V13h-4v1.5H11z M11,9h2v1.5h-2V9z M11,6h2v1.5h-2V6z M11,12h2v-1.5h-2V12z"/></svg>');
}

/* 响应式调整 */
@media (max-width: 1024px) {
  .download-actions {
    width: 100%;
  }
  
  .download-all-btn {
    width: 100%;
  }
}
</style> 