<template>
  <a-modal
    :visible="visible"
    :title="$t('设置主机名')"
    :width="650"
    :maskClosable="false"
    :destroyOnClose="true"
    @cancel="handleCancel"
    :footer="null"
    class="hostname-setting-modal"
  >
    <div v-if="!taskId">
      <a-form :form="form" layout="vertical" @change="updatePreview">
        <a-form-item :label="$t('主机名前缀')" :colon="false">
          <a-select
            v-decorator="[
              'prefix',
              {
                initialValue: 'bigdata',
                rules: [{ required: false, message: $t('请选择或输入主机名前缀') }]
              }
            ]"
            :placeholder="$t('请选择或输入主机名前缀')"
            allow-clear
            show-search
            @change="updatePreview"
            @blur="updatePreview"
          >
            <a-select-option v-for="item in prefixOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item :label="$t('编号样式')" :colon="false">
          <a-select
            v-decorator="[
              'zeroCount',
              {
                initialValue: 3,
                rules: [{ required: false, message: $t('请选择编号样式') }]
              }
            ]"
            :placeholder="$t('请选择编号样式')"
            style="width: 100%"
            @change="updatePreview"
            @blur="updatePreview"
          >
            <a-select-option v-for="item in numberFormatOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item :label="$t('分隔符')" :colon="false">
          <a-select
            v-decorator="[
              'separator',
              {
                initialValue: '',
                rules: [{ required: false }]
              }
            ]"
            :placeholder="$t('请选择或输入分隔符')"
            allow-clear
            show-search
            @change="updatePreview"
            @blur="updatePreview"
          >
            <a-select-option v-for="item in separatorOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item :label="$t('后缀')" :colon="false">
          <a-input
            v-decorator="[
              'suffix',
              {
                initialValue: '',
                rules: [{ required: false }]
              }
            ]"
            :placeholder="$t('请输入后缀（可选）')"
            allow-clear
            @change="updatePreview"
            @input="updatePreview"
            @blur="updatePreview"
          />
        </a-form-item>
        
        <div class="preview-box">
          <div class="preview-title">{{ $t('预览') }}</div>
          <div class="preview-content">
            <div class="example-line">
              <span class="example-label">{{ $t('示例1：') }}</span>
              <span class="example-value">{{ previewExample1 }}</span>
            </div>
            <div class="example-line">
              <span class="example-label">{{ $t('示例2：') }}</span>
              <span class="example-value">{{ previewExample2 }}</span>
            </div>
            <div class="example-line">
              <span class="example-label">{{ $t('示例3：') }}</span>
              <span class="example-value">{{ previewExample3 }}</span>
            </div>
          </div>
        </div>

        <div class="action-buttons">
          <a-button @click="handleCancel">{{ $t('取消') }}</a-button>
          <a-button 
            type="primary"
            :loading="saveLoading"
            @click="handleSave"
          >{{ $t('保存') }}</a-button>
          <a-button
            type="primary"
            :loading="saveAndSyncLoading"
            @click="handleSaveAndSync"
          >{{ $t('保存并同步hosts') }}</a-button>
        </div>
      </a-form>
    </div>
    
    <!-- 任务进度卡片 -->
    <div v-if="taskId" class="progress-card">
      <div class="card-title">{{ $t('设置进度') }}</div>
      
      <div class="progress-content">
        <!-- 进度条和状态 -->
        <div class="progress-status">
          <div class="status-header">
            <div class="status-title" v-if="taskStatus === 'IN_PROGRESS'">
              <a-icon type="sync" spin class="status-icon in-progress" />
              <span>{{ $t('正在设置主机名') }}</span>
            </div>
            <div class="status-title" v-else-if="taskStatus === 'COMPLETED'">
              <a-icon type="check-circle" class="status-icon completed" />
              <span>{{ $t('设置完成') }}</span>
            </div>
            <div class="status-title" v-else-if="taskStatus === 'FAILED'">
              <a-icon type="close-circle" class="status-icon failed" />
              <span>{{ $t('设置失败') }}</span>
            </div>
            
            <div class="status-stats">
              <div class="stat-item completed">
                <div class="stat-value">{{ completedCount }}</div>
                <div class="stat-label">{{ $t('成功') }}</div>
              </div>
              <div class="stat-item failed" v-if="failedCount > 0">
                <div class="stat-value">{{ failedCount }}</div>
                <div class="stat-label">{{ $t('失败') }}</div>
              </div>
            </div>
          </div>
          
          <a-progress 
            :percent="percentage" 
            :status="taskStatus === 'FAILED' ? 'exception' : taskStatus === 'COMPLETED' ? 'success' : 'active'"
            :strokeColor="taskStatus === 'FAILED' ? '#ff4d4f' : taskStatus === 'COMPLETED' ? '#52c41a' : '#1890ff'"
            :strokeWidth="6"
          />
          
          <!-- 当前处理的主机 -->
          <div class="current-host" v-if="currentHost && taskStatus === 'IN_PROGRESS'">
            <a-tag color="processing">{{ $t('正在设置') }}: {{ currentHost }}</a-tag>
          </div>
          
          <!-- 消息通知 -->
          <div class="task-message" v-if="taskMessage">
            {{ taskMessage }}
          </div>
        </div>
        
        <!-- 完成的主机列表 -->
        <a-collapse 
          v-if="completedHosts.length > 0" 
          class="hosts-collapse"
          expandIconPosition="right"
        >
          <a-collapse-panel :header="$t('已完成的主机') + ' (' + completedHosts.length + ')'" key="1" class="apple-collapse-panel">      
            <div class="hosts-list">
              <a-tag 
                v-for="host in completedHosts" 
                :key="host" 
                class="host-tag success-host-tag"
              >
                {{ host }}
              </a-tag>
            </div>
          </a-collapse-panel>
        </a-collapse>
        
        <!-- 待设置主机列表 -->
        <a-collapse 
          v-if="pendingHosts && pendingHosts.length > 0" 
          class="hosts-collapse"
          expandIconPosition="right"
        >
          <a-collapse-panel :header="$t('待设置主机') + ' (' + pendingHosts.length + ')'" key="3" class="apple-collapse-panel pending-panel">
            <div class="hosts-list">
              <a-tag 
                v-for="host in pendingHosts" 
                :key="host" 
                class="host-tag pending-host-tag"
              >
                {{ host }}
              </a-tag>
            </div>
          </a-collapse-panel>
        </a-collapse>
        
        <!-- 失败的主机列表 -->
        <a-collapse 
          v-if="failedHosts && Object.keys(failedHosts).length > 0" 
          class="hosts-collapse"
          expandIconPosition="right"
        >
          <a-collapse-panel :header="$t('失败的主机') + ' (' + Object.keys(failedHosts).length + ')'" key="2" class="apple-collapse-panel error-panel">
            <div class="failed-hosts-list">
              <div class="failed-host-item" v-for="(reason, ip) in failedHosts" :key="ip">
                <a-tooltip placement="top" :title="reason">
                  <div class="failed-host-ip">{{ ip }}</div>
                </a-tooltip>
                <div class="failed-host-reason">{{ shortenReason(reason) }}</div>
              </div>
            </div>
          </a-collapse-panel>
        </a-collapse>
      </div>
      
      <div class="progress-actions">
        <a-button @click="handleCancel" class="cancel-button">{{ $t('关闭') }}</a-button>
      </div>
    </div>
  </a-modal>
</template>

<script>
import HostCheckService from './HostCheckService'
import TaskStateManager, { TASK_TYPE } from './TaskStateManager'

export default {
  name: 'HostnameSettingModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    clusterId: {
      type: [Number, String],
      required: true
    }
  },
  data() {
    return {
      form: this.$form.createForm(this),
      saveLoading: false,
      saveAndSyncLoading: false,
      prefixOptions: [
        { label: 'bigdata', value: 'bigdata' },
        { label: 'hadoop', value: 'hadoop' },
        { label: 'spark', value: 'spark' },
        { label: 'hive', value: 'hive' },
        { label: 'flink', value: 'flink' },
        { label: 'kafka', value: 'kafka' },
        { label: 'datanode', value: 'datanode' },
        { label: 'node', value: 'node' },
        { label: 'server', value: 'server' },
        { label: 'host', value: 'host' }
      ],
      separatorOptions: [
        { label: '无', value: '' },
        { label: '-', value: '-' },
        { label: '_', value: '_' },
        { label: '.', value: '.' }
      ],
      numberFormatOptions: [
        { label: '一位数字 (例如: 1、2...)', value: 1 },
        { label: '两位数字 (例如: 01、02...)', value: 2 },
        { label: '三位数字 (例如: 001、002...)', value: 3 },
        { label: '四位数字 (例如: 0001、0002...)', value: 4 },
        { label: '五位数字 (例如: 00001、00002...)', value: 5 }
      ],
      // 预览示例
      previewExample1: 'bigdata001',
      previewExample2: 'bigdata002',
      previewExample3: 'bigdata003',
      
      // 任务相关
      taskId: null,
      taskStatus: null,
      completedCount: 0,
      failedCount: 0,
      percentage: 0,
      currentHost: null,
      taskMessage: null,
      completedHosts: [],
      failedHosts: {},
      pendingHosts: [],
      
      // 轮询任务
      pollingTimer: null
    }
  },
  watch: {
    visible(val) {
      if (val) {
        // 打开弹窗时，检查是否有正在进行的任务
        this.checkExistingTask();
      } else {
        // 清除轮询定时器
        this.clearPollingTimer();
      }
    }
  },
  mounted() {
    // 初始化表单后更新预览
    this.$nextTick(() => {
      this.updatePreview();
    });
  },
  beforeDestroy() {
    this.clearPollingTimer();
  },
  methods: {
    // 检查是否有正在进行的任务
    async checkExistingTask() {
      // 获取保存的任务ID
      const savedTaskId = TaskStateManager.getTaskId(TASK_TYPE.HOSTNAME_SETTING, this.clusterId);
      
      if (savedTaskId) {
        // 设置任务ID并开始轮询进度
        this.taskId = savedTaskId;
        this.startPollingTaskProgress(savedTaskId);
      } else {
        // 没有进行中的任务，重置状态
        this.resetState();
      }
    },
    
    // 重置状态
    resetState() {
      this.form.resetFields();
      
      this.taskId = null;
      this.taskStatus = null;
      this.completedCount = 0;
      this.failedCount = 0;
      this.percentage = 0;
      this.currentHost = null;
      this.taskMessage = null;
      this.completedHosts = [];
      this.failedHosts = {};
      this.pendingHosts = [];
      
      this.clearPollingTimer();
      
      // 初始化预览
      this.updatePreview();
    },
    
    // 清除轮询定时器
    clearPollingTimer() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer);
        this.pollingTimer = null;
      }
    },
    
    // 获取表单值
    getFormValues() {
      return new Promise((resolve, reject) => {
        this.form.validateFields((err, values) => {
          if (err) {
            reject(err)
          } else {
            resolve(values)
          }
        })
      })
    },
    
    // 更新预览示例
    updatePreview() {
      try {
        this.$nextTick(() => {
          // 获取当前表单值（不使用异步方法，避免表单验证）
          const formValues = this.form.getFieldsValue();
          const prefix = formValues.prefix || 'bigdata';
          const zeroCount = formValues.zeroCount || 3;
          const separator = formValues.separator || '';
          const suffix = formValues.suffix || '';
          
          // 生成预览示例
          this.previewExample1 = this.generateExampleHostname(prefix, zeroCount, separator, suffix, 1);
          this.previewExample2 = this.generateExampleHostname(prefix, zeroCount, separator, suffix, 2);
          this.previewExample3 = this.generateExampleHostname(prefix, zeroCount, separator, suffix, 3);
        });
      } catch (e) {
        console.error('Update preview error:', e);
      }
    },
    
    // 生成示例主机名
    generateExampleHostname(prefix, zeroCount, separator, suffix, index) {
      const paddedNum = index.toString().padStart(zeroCount, '0');
      return prefix + separator + paddedNum + suffix;
    },
    
    // 开始轮询任务进度
    startPollingTaskProgress(taskId) {
      this.clearPollingTimer();
      this.taskId = taskId;
      
      // 保存任务ID到全局状态
      TaskStateManager.setTaskId(TASK_TYPE.HOSTNAME_SETTING, taskId, this.clusterId);
      
      // 立即执行一次
      this.pollTaskProgress();
      
      // 每1秒轮询一次
      this.pollingTimer = setInterval(() => {
        this.pollTaskProgress();
      }, 1000);
    },
    
    // 轮询任务进度
    async pollTaskProgress() {
      if (!this.taskId) return;
      
      try {
        const res = await HostCheckService.getTaskProgress(this, this.taskId);
        
        if (res.code === 200) {
          const progress = res.data;
          
          // 更新任务状态
          this.taskStatus = progress.status;
          this.completedCount = progress.completedCount;
          this.failedCount = progress.failedCount;
          this.percentage = progress.percentage;
          this.currentHost = progress.currentHost;
          this.taskMessage = progress.message;
          
          if (progress.completedHosts) {
            this.completedHosts = progress.completedHosts;
          }
          
          if (progress.failedHosts) {
            this.failedHosts = progress.failedHosts;
          }
          
          if (progress.pendingHosts) {
            this.pendingHosts = progress.pendingHosts;
          }
          
          // 如果任务已完成，停止轮询
          if (progress.status === 'COMPLETED' || progress.status === 'FAILED') {
            this.clearPollingTimer();
            // 清除任务状态
            TaskStateManager.clearTaskId(TASK_TYPE.HOSTNAME_SETTING);
          }
        } else {
          console.error('Poll task progress error:', res.msg);
          // 尝试5次后如果仍然失败，停止轮询
          this.failCount = (this.failCount || 0) + 1;
          if (this.failCount >= 5) {
            this.clearPollingTimer();
            this.taskStatus = 'FAILED';
            this.taskMessage = res.msg || this.$t('获取任务进度失败');
            // 清除任务状态
            TaskStateManager.clearTaskId(TASK_TYPE.HOSTNAME_SETTING);
          }
        }
      } catch (e) {
        console.error('Poll task progress error:', e);
      }
    },
    
    // 获取所有主机列表
    async getAllHosts() {
      try {
        const res = await HostCheckService.getHostList(this, this.clusterId);
        if (res.code === 200 && res.data) {
          // 将所有主机IP添加到allHosts数组
          this.allHosts = res.data.map(host => host.ip);
          
          // 初始时，所有主机都是待处理状态
          this.pendingHosts = [...this.allHosts];
          
          return this.allHosts;
        }
      } catch (e) {
        console.error('Get host list error:', e);
      }
      return [];
    },
    
    // 更新待设置主机列表
    updatePendingHosts() {
      if (!this.allHosts || !Array.isArray(this.allHosts) || this.allHosts.length === 0) return;
      
      // 计算待设置主机 = 所有主机 - 已完成主机 - 失败主机
      const completedSet = new Set(this.completedHosts);
      const failedSet = new Set(Object.keys(this.failedHosts));
      
      this.pendingHosts = this.allHosts.filter(host => 
        !completedSet.has(host) && !failedSet.has(host)
      );
    },
    
    // 保存主机名
    async handleSave() {
      try {
        this.saveLoading = true;
        const values = await this.getFormValues();
        
        const res = await HostCheckService.batchSetHostname(
          this,
          this.clusterId,
          values.prefix || 'bigdata',
          values.zeroCount || 3,
          values.separator || '',
          values.suffix || ''
        );
        
        if (res.code === 200) {
          // 开始轮询任务进度
          this.startPollingTaskProgress(res.data);
        } else {
          this.$message.error(res.msg || this.$t('主机名设置失败'));
        }
      } catch (e) {
        console.error('Save error:', e);
        this.$message.error(e.message || this.$t('主机名设置失败'));
      } finally {
        this.saveLoading = false;
      }
    },

    // 保存并同步hosts文件
    async handleSaveAndSync() {
      try {
        this.saveAndSyncLoading = true;
        // 先保存主机名
        const values = await this.getFormValues();
        
        const saveRes = await HostCheckService.batchSetHostname(
          this,
          this.clusterId,
          values.prefix || 'bigdata',
          values.zeroCount || 3,
          values.separator || '',
          values.suffix || ''
        );
        
        if (saveRes.code === 200) {
          // 开始轮询任务进度
          this.startPollingTaskProgress(saveRes.data);
        } else {
          this.$message.error(saveRes.msg || this.$t('主机名设置失败'));
        }
      } catch (e) {
        console.error('Save and sync error:', e);
        this.$message.error(e.message || this.$t('操作失败'));
      } finally {
        this.saveAndSyncLoading = false;
      }
    },
    
    // 取消或关闭弹窗
    handleCancel() {
      this.$emit('close');
    },

    shortenReason(reason) {
      if (!reason) return '';
      if (reason.length <= 70) return reason;
      return reason.substring(0, 70) + '...';
    }
  }
}
</script>

<style lang="less" scoped>
.hostname-setting-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.feature-description {
  display: flex;
  align-items: flex-start;
  background-color: #f5f5f7;
  border-radius: 12px;
  padding: 16px;
}

.description-icon {
  color: #0071e3;
  font-size: 22px;
  margin-right: 12px;
  margin-top: 2px;
}

.description-title {
  font-weight: 600;
  margin-bottom: 8px;
  font-size: 16px;
  color: #1d1d1f;
}

.description-text {
  color: #6e6e73;
  line-height: 1.5;
  font-size: 14px;
}

.hostname-card-container {
  display: flex;
  gap: 24px;
}

.hostname-form-card {
  flex: 1;
  background-color: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.hostname-preview-card {
  flex: 1;
  background-color: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.card-title {
  font-weight: 600;
  margin-bottom: 16px;
  font-size: 16px;
  color: #1d1d1f;
  position: relative;
}

.card-title:after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 0;
  width: 40px;
  height: 2px;
  background-color: #0071e3;
}

.preview-content {
  height: 100%;
  border: none;
  background-color: #fff;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.preview-header {
  margin-bottom: 20px;
}

.preview-subtitle {
  font-weight: 600;
  margin-bottom: 4px;
  font-size: 15px;
  color: #1d1d1f;
}

.preview-info {
  font-size: 13px;
  color: #6e6e73;
}

.preview-examples {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background-color: #f5f5f7;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
}

.preview-example {
  display: flex;
  align-items: center;
  background-color: #fff;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s ease;
}

.preview-example:hover {
  transform: translateY(-2px);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.08);
}

.example-number {
  width: 24px;
  height: 24px;
  background-color: #0071e3;
  color: white;
  font-size: 14px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: 12px;
  font-weight: 500;
}

.example-hostname {
  font-family: "SF Mono", "Consolas", "Monaco", monospace;
  font-size: 15px;
  color: #1d1d1f;
  letter-spacing: 0.5px;
}

.preview-more {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  font-size: 14px;
  color: #6e6e73;
  gap: 8px;
}

.preview-tips {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  font-size: 14px;
  color: #6e6e73;
  background-color: #f5f5f7;
  border-radius: 8px;
  padding: 12px;
}

.preview-tips .anticon {
  color: #0071e3;
}

.hostname-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.save-button,
.save-sync-button {
  background-color: #0071e3;
  border-color: #0071e3;
  border-radius: 8px;
  padding: 0 20px;
  height: 38px;
  font-weight: 500;
}

.save-button:hover,
.save-button:focus,
.save-sync-button:hover,
.save-sync-button:focus {
  background-color: #0077ED;
  border-color: #0077ED;
}

.preview-box {
  margin-top: 20px;
  margin-bottom: 20px;
  background-color: #f5f7fa;
  border-radius: 6px;
  padding: 15px;
}

.preview-title {
  font-weight: 600;
  margin-bottom: 15px;
}

.preview-content {
  background-color: #fff;
  border-radius: 4px;
  padding: 15px;
}

.example-line {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.example-label {
  color: #666;
  width: 80px;
}

.example-value {
  font-family: Consolas, Monaco, 'Andale Mono', monospace;
  font-weight: 500;
  color: #1890ff;
}

.action-buttons {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

.action-buttons button {
  margin-left: 12px;
}

/* 进度卡片样式 */
.progress-card {
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.card-title {
  font-weight: 600;
  margin-bottom: 20px;
  font-size: 18px;
  color: #1d1d1f;
  position: relative;
}

.progress-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.progress-status {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.status-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 500;
}

.status-icon {
  font-size: 20px;
}

.in-progress {
  color: #0071e3;
}

.completed {
  color: #34c759;
}

.failed {
  color: #ff3b30;
}

.status-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 14px;
  border-radius: 10px;
  transition: all 0.3s ease;
}

.stat-item.completed {
  background-color: rgba(52, 199, 89, 0.1);
}

.stat-item.failed {
  background-color: rgba(255, 59, 48, 0.1);
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 4px;
}

.stat-item.completed .stat-value {
  color: #34c759;
}

.stat-item.failed .stat-value {
  color: #ff3b30;
}

.stat-label {
  font-size: 14px;
  color: #6e6e73;
}

.current-host {
  margin-top: 16px;
}

/deep/ .ant-tag {
  border-radius: 6px;
  font-size: 13px;
  padding: 4px 10px;
  border: none;
  font-weight: 500;
}

.task-message {
  background-color: rgba(245, 247, 250, 0.7);
  padding: 16px;
  border-radius: 12px;
  color: #6e6e73;
  font-size: 14px;
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  animation: fadeIn 0.3s ease-in-out;
}

.hosts-collapse {
  border: none;
  background-color: transparent;
}

/deep/ .apple-collapse-panel {
  border: none !important;
  border-radius: 12px !important;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1.0);
}

/deep/ .apple-collapse-panel.error-panel .ant-collapse-header {
  background-color: rgba(255, 59, 48, 0.08);
  color: #1d1d1f;
}

/deep/ .ant-collapse-header {
  padding: 14px 16px !important;
  background-color: rgba(0, 113, 227, 0.08);
  border-radius: 12px !important;
  font-weight: 500;
  color: #1d1d1f !important;
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
}

/deep/ .ant-collapse-header:hover {
  background-color: rgba(0, 113, 227, 0.12);
}

/deep/ .ant-collapse-arrow {
  font-size: 14px !important;
  color: #0071e3 !important;
  transition: transform 0.3s cubic-bezier(0.25, 0.1, 0.25, 1.0) !important;
}

/deep/ .ant-collapse-item-active .ant-collapse-header {
  border-bottom-left-radius: 0 !important;
  border-bottom-right-radius: 0 !important;
}

/deep/ .ant-collapse-content {
  border-top: none;
  background-color: rgba(250, 250, 252, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  animation: slideDown 0.3s cubic-bezier(0.25, 0.1, 0.25, 1.0);
}

/deep/ .ant-collapse-content-box {
  padding: 16px !important;
}

.hosts-list {
  padding: 12px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.host-tag {
  padding: 6px 12px;
  border-radius: 8px;
  font-family: "SF Mono", "Consolas", "Monaco", monospace;
  letter-spacing: 0.3px;
  transition: all 0.3s ease;
  border: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.success-host-tag {
  background-color: rgba(52, 199, 89, 0.15);
  color: #116329;
}

.success-host-tag:hover {
  background-color: rgba(52, 199, 89, 0.25);
  transform: translateY(-1px);
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.08);
}

.failed-hosts-list {
  padding: 12px 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.failed-host-item {
  padding: 14px;
  border-radius: 10px;
  background-color: rgba(255, 59, 48, 0.08);
  border: 1px solid rgba(255, 59, 48, 0.15);
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: all 0.3s ease;
}

.failed-host-item:hover {
  background-color: rgba(255, 59, 48, 0.12);
  transform: translateY(-1px);
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.06);
}

.failed-host-ip {
  font-weight: 500;
  color: #1d1d1f;
  font-family: "SF Mono", "Consolas", "Monaco", monospace;
  font-size: 14px;
}

.failed-host-reason {
  color: #ff3b30;
  font-size: 13px;
}

.progress-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.cancel-button {
  border-radius: 10px;
  font-size: 14px;
  height: 38px;
  padding: 0 18px;
  border: none;
  background-color: rgba(242, 242, 242, 0.9);
  color: #1d1d1f;
  font-weight: 500;
  transition: all 0.3s ease;
}

.cancel-button:hover {
  background-color: rgba(230, 230, 230, 0.9);
  transform: translateY(-1px);
}

.sync-button {
  border-radius: 10px;
  font-size: 14px;
  height: 38px;
  padding: 0 18px;
  border: none;
  background-color: #0071e3;
  color: white;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 6px rgba(0, 113, 227, 0.3);
}

.sync-button:hover {
  background-color: #0077ED;
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(0, 113, 227, 0.4);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    max-height: 0;
  }
  to {
    opacity: 1;
    max-height: 1000px;
  }
}

.pending-host-tag {
  background-color: rgba(255, 149, 0, 0.15);
  color: #804000;
}

.pending-host-tag:hover {
  background-color: rgba(255, 149, 0, 0.25);
  transform: translateY(-1px);
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.08);
}

/deep/ .apple-collapse-panel.pending-panel .ant-collapse-header {
  background-color: rgba(255, 149, 0, 0.08);
  color: #1d1d1f;
}
</style>

<style>
/* 全局样式，使用苹果风格 */
.hostname-setting-modal .ant-modal-content {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.12);
}

.hostname-setting-modal .ant-modal-header {
  background-color: #ffffff;
  border-bottom: none;
  padding: 24px 24px 0;
}

.hostname-setting-modal .ant-modal-title {
  font-weight: 600;
  font-size: 20px;
  color: #1d1d1f;
  text-align: center;
}

.hostname-setting-modal .ant-modal-body {
  padding: 24px;
  background-color: #ffffff;
}

.hostname-setting-modal .ant-form-item-label label {
  color: #1d1d1f;
  font-weight: 500;
  font-size: 14px;
}

.hostname-setting-modal .ant-select-selection,
.hostname-setting-modal .ant-input,
.hostname-setting-modal .ant-input-number {
  border-radius: 8px;
  padding: 8px 12px;
  height: auto;
  border-color: #d2d2d7;
  transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
}

.hostname-setting-modal .ant-select-selection:hover,
.hostname-setting-modal .ant-input:hover,
.hostname-setting-modal .ant-input-number:hover {
  border-color: #0071e3;
}

.hostname-setting-modal .ant-select-focused .ant-select-selection,
.hostname-setting-modal .ant-input:focus,
.hostname-setting-modal .ant-input-number-focused {
  border-color: #0071e3;
  box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
}

.hostname-setting-modal .ant-btn {
  border-radius: 8px;
  font-size: 14px;
  height: 38px;
  padding: 0 18px;
  transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
}

.hostname-setting-modal .ant-select-dropdown {
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.hostname-setting-modal .ant-select-dropdown-menu-item {
  padding: 10px 12px;
  transition: all 0.2s;
}

.hostname-setting-modal .ant-select-dropdown-menu-item:hover {
  background-color: #f5f5f7;
}

.hostname-setting-modal .ant-select-dropdown-menu-item-selected {
  color: #0071e3;
  background-color: rgba(0, 113, 227, 0.05);
  font-weight: 500;
}
</style> 