<template>
  <a-modal
    :visible="visible"
    :title="$t('同步Hosts文件')"
    :width="700"
    :maskClosable="false"
    :destroyOnClose="true"
    @cancel="handleCancel"
    :footer="null"
    class="sync-hosts-modal"
  >
    <div class="sync-hosts-container">
      <div class="feature-description">
        <a-alert type="info" show-icon>
          <span slot="message">
            <div class="description-title">{{ $t('功能说明') }}</div>
            <div class="description-content">
              {{ $t('该功能会自动生成包含所有主机IP和主机名的hosts文件，并将其同步到所有集群主机。同步后，主机之间可以通过主机名直接通信，无需记忆IP地址。') }}
            </div>
          </span>
        </a-alert>
      </div>
      
      <div class="hosts-preview-container">
        <a-spin :spinning="loading">
          <div class="hosts-preview-header">
            <div class="hosts-preview-title">{{ $t('Hosts文件预览') }}</div>
            <div class="hosts-preview-info" v-if="previewData">
              <a-tag color="blue">{{ previewData.hostCount }} {{ $t('台主机') }}</a-tag>
            </div>
          </div>
          
          <div class="hosts-content-wrapper">
            <a-input.TextArea
              v-model="hostsContent"
              :rows="15"
              readonly
              :placeholder="$t('Hosts文件内容')"
              class="hosts-content"
            />
          </div>
          
          <div class="sync-result" v-if="syncResult">
            <div class="sync-result-header">
              <div class="sync-result-title">{{ $t('同步结果') }}</div>
              <div class="sync-result-info">
                <a-tag color="green" v-if="syncResult.successCount">{{ syncResult.successCount }} {{ $t('台成功') }}</a-tag>
                <a-tag color="red" v-if="syncResult.failedCount">{{ syncResult.failedCount }} {{ $t('台失败') }}</a-tag>
              </div>
            </div>
            
            <a-collapse v-if="syncResult.failedCount > 0" class="failed-hosts-collapse">
              <a-collapse-panel :header="$t('查看失败主机')" key="1">
                <a-list
                  size="small"
                  :dataSource="Object.entries(syncResult.failedHosts)"
                  :pagination="false"
                >
                  <a-list-item slot="renderItem" slot-scope="item">
                    <span class="failed-host-ip">{{ item[0] }}</span>
                    <span class="failed-host-reason">{{ item[1] }}</span>
                  </a-list-item>
                </a-list>
              </a-collapse-panel>
            </a-collapse>
          </div>
        </a-spin>
      </div>

      <div class="hosts-actions">
        <a-button @click="handleCancel">{{ $t('取消') }}</a-button>
        <a-button
          type="primary"
          :loading="syncInProgress"
          :disabled="!previewData || syncInProgress"
          @click="handleSync"
        >{{ $t('同步到所有主机') }}</a-button>
      </div>
    </div>
  </a-modal>
</template>

<script>
import HostCheckService from './HostCheckService'

export default {
  name: 'SyncHostsFileModal',
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
      loading: false,
      syncInProgress: false,
      previewData: null,
      hostsContent: '',
      syncResult: null
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.generatePreview()
      }
    }
  },
  methods: {
    // 生成hosts文件预览
    async generatePreview() {
      try {
        this.loading = true
        this.syncResult = null
        
        const res = await HostCheckService.generateHostsFilePreview(this, this.clusterId)
        
        if (res.code === 200) {
          this.previewData = res.data
          this.hostsContent = res.data.hostsContent
        } else {
          this.$message.error(res.msg || this.$t('生成预览失败'))
        }
      } catch (e) {
        console.error('Generate preview error:', e)
        this.$message.error(this.$t('生成预览失败'))
      } finally {
        this.loading = false
      }
    },

    // 同步hosts文件到所有主机
    async handleSync() {
      try {
        this.syncInProgress = true
        
        const res = await HostCheckService.syncHostsFile(this, this.clusterId)
        
        if (res.code === 200) {
          this.syncResult = res.data
          if (res.data.failedCount === 0) {
            this.$message.success(this.$t('同步hosts文件成功'))
            this.$emit('success')
          } else {
            this.$message.warning(this.$t('部分主机同步失败，请查看详情'))
          }
        } else {
          this.$message.error(res.msg || this.$t('同步hosts文件失败'))
        }
      } catch (e) {
        console.error('Sync hosts error:', e)
        this.$message.error(this.$t('同步hosts文件失败'))
      } finally {
        this.syncInProgress = false
      }
    },

    // 取消
    handleCancel() {
      this.$emit('close')
    }
  }
}
</script>

<style lang="less" scoped>
.sync-hosts-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-description {
  margin-bottom: 8px;
}

.description-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.description-content {
  color: #666;
  line-height: 1.5;
}

.hosts-preview-container {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.hosts-preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #f5f5f7;
  border-bottom: 1px solid #f0f0f0;
}

.hosts-preview-title {
  font-weight: 500;
  font-size: 15px;
  color: #1d1d1f;
}

.hosts-content-wrapper {
  padding: 16px;
  background-color: #fafafa;
}

.hosts-content {
  width: 100%;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  resize: none;
  background-color: #fff;
  border-radius: 6px;
}

.sync-result {
  padding: 0 16px 16px;
}

.sync-result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.sync-result-title {
  font-weight: 500;
  font-size: 15px;
  color: #1d1d1f;
}

.failed-hosts-collapse {
  margin-top: 8px;
}

.failed-host-ip {
  font-weight: 500;
  color: #1d1d1f;
  margin-right: 16px;
}

.failed-host-reason {
  color: #ff4d4f;
}

.hosts-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
</style>

<style>
/* 全局样式，使用苹果风格 */
.sync-hosts-modal .ant-modal-content {
  border-radius: 10px;
  overflow: hidden;
}

.sync-hosts-modal .ant-modal-header {
  background-color: #f5f5f7;
  border-bottom: none;
  padding: 16px 24px;
}

.sync-hosts-modal .ant-modal-title {
  font-weight: 500;
  font-size: 18px;
  color: #1d1d1f;
}

.sync-hosts-modal .ant-modal-body {
  padding: 24px;
  background-color: #fff;
}

.sync-hosts-modal .ant-btn-primary {
  background-color: #0071e3;
  border-color: #0071e3;
}

.sync-hosts-modal .ant-btn-primary:hover, 
.sync-hosts-modal .ant-btn-primary:focus {
  background-color: #0077ED;
  border-color: #0077ED;
}

.sync-hosts-modal .ant-collapse {
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.sync-hosts-modal .ant-collapse-header {
  padding: 8px 16px !important;
  color: #1d1d1f !important;
}

.sync-hosts-modal .ant-collapse-content {
  border-top: 1px solid #f0f0f0;
}

.sync-hosts-modal .ant-collapse-content-box {
  padding: 8px 16px !important;
}

.sync-hosts-modal .ant-list-item {
  padding: 8px 0;
  display: flex;
  justify-content: space-between;
}
</style> 