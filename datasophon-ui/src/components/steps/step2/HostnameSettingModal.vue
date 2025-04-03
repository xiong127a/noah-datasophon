<template>
  <a-modal
    :visible="visible"
    :title="$t('设置主机名')"
    :width="750"
    :maskClosable="false"
    :destroyOnClose="true"
    @cancel="handleCancel"
    :footer="null"
    class="hostname-setting-modal"
  >
    <div class="hostname-setting-container">
      <!-- 功能介绍 -->
      <div class="feature-description">
        <div class="description-icon">
          <a-icon type="bulb" />
        </div>
        <div class="description-content">
          <div class="description-title">{{ $t('功能说明') }}</div>
          <div class="description-text">
            {{ $t('该功能可以批量设置集群主机名，支持自定义前缀、数字编号样式、分隔符和后缀。系统会根据规则自动为每台主机生成唯一的主机名，便于识别和管理。支持最多5位数字编号，可满足10万台主机的命名需求。') }}
          </div>
        </div>
      </div>
      
      <div class="hostname-card-container">
        <!-- 左侧设置卡片 -->
        <div class="hostname-form-card">
          <div class="card-title">{{ $t('命名规则设置') }}</div>
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
          </a-form>
        </div>

        <!-- 右侧预览卡片 -->
        <div class="hostname-preview-card">
          <div class="card-title">{{ $t('主机名预览') }}</div>
          <div class="preview-content">
            <div class="preview-header">
              <div class="preview-subtitle">{{ $t('生成结果示例') }}</div>
              <div class="preview-info">{{ $t('将按此格式为所有主机依次设置') }}</div>
            </div>
            
            <div class="preview-examples">
              <div class="preview-example">
                <div class="example-number">1</div>
                <div class="example-hostname">{{ previewExample1 }}</div>
              </div>
              <div class="preview-example">
                <div class="example-number">2</div>
                <div class="example-hostname">{{ previewExample2 }}</div>
              </div>
              <div class="preview-example">
                <div class="example-number">3</div>
                <div class="example-hostname">{{ previewExample3 }}</div>
              </div>
              
              <div class="preview-more">
                <a-icon type="ellipsis" />
                <span>{{ $t('更多主机将依此类推') }}</span>
              </div>
            </div>
            
            <div class="preview-tips">
              <a-icon type="info-circle" />
              <span>{{ $t('主机名将按照设置的规则自动递增') }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="hostname-actions">
        <a-button @click="handleCancel">{{ $t('取消') }}</a-button>
        <a-button
          type="primary"
          :loading="saveLoading"
          @click="handleSave"
          class="save-button"
        >{{ $t('保存') }}</a-button>
        <a-button
          type="primary"
          :loading="saveAndSyncLoading"
          @click="handleSaveAndSync"
          class="save-sync-button"
        >{{ $t('保存并同步hosts') }}</a-button>
      </div>
    </div>
  </a-modal>
</template>

<script>
import HostCheckService from './HostCheckService'

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
      previewExample3: 'bigdata003'
    }
  },
  watch: {
    visible(val) {
      if (val) {
        // 表单重置
        this.form.resetFields();
        // 初始化预览
        this.updatePreview();
      }
    }
  },
  mounted() {
    // 初始化表单后更新预览
    this.$nextTick(() => {
      this.updatePreview();
    });
  },
  methods: {
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
          this.$message.success(res.data?.message || this.$t('主机名设置成功'));
          this.$emit('success');
          this.$emit('close');
        } else {
          this.$message.error(res.msg || this.$t('主机名设置失败'));
        }
      } catch (e) {
        console.error('Save error:', e);
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
          // 再同步hosts文件
          this.$emit('syncHosts', this.clusterId);
          this.$emit('success');
          this.$emit('close');
        } else {
          this.$message.error(saveRes.msg || this.$t('主机名设置失败'));
        }
      } catch (e) {
        console.error('Save and sync error:', e);
      } finally {
        this.saveAndSyncLoading = false;
      }
    },

    // 取消
    handleCancel() {
      this.$emit('close');
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